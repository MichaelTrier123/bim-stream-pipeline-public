package dk.trier.bim_stream_worker;

import co.elastic.apm.api.ElasticApm;
import co.elastic.apm.api.Outcome;
import co.elastic.apm.api.Scope;
import co.elastic.apm.api.Transaction;
import dk.trier.bim_stream_worker.persistence.IfcModelStats;
import dk.trier.bim_stream_worker.service.ImportPersistenceService;
import io.awspring.cloud.sqs.annotation.SqsListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.eventnotifications.s3.model.S3EventNotification;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Map;
import java.util.UUID;

@Service
public class SqsWorker {

    private static final Logger log = LoggerFactory.getLogger(SqsWorker.class);
    private final S3Client s3Client;
    private final RestTemplate restTemplate;
    private final Path sharedVolume = Paths.get("/data");
    private final ImportPersistenceService persistenceService;

    @Value("${app.s3.input-bucket}")
    private String inputBucket;

    @Value("${bimstream.processor.url}")
    private String pythonUrl;

    public SqsWorker(S3Client s3Client, RestTemplate restTemplate, ImportPersistenceService persistenceService) {
        this.s3Client = s3Client;
        this.restTemplate = restTemplate;
        this.persistenceService = persistenceService;
    }

    @SqsListener("${app.sqs.queue-url}")
    public void handleMessage(String messageJson) {
        Transaction tx = ElasticApm.startTransaction();

        String workId = UUID.randomUUID().toString();
        Path workDir = sharedVolume.resolve(workId);

        try (Scope scope = tx.activate()) {
            tx.setName("IFC job");
            tx.setType("messaging");
            Files.createDirectories(workDir);
            log.info("Starting new task: {}", workId);

            String s3Key = extractS3Key(messageJson);
            Path localFilePath = workDir.resolve("model.ifc");

            log.info("Downloading {} to {}", s3Key, localFilePath);
            s3Client.getObject(
                    req -> req.bucket(inputBucket).key(s3Key),
                    ResponseTransformer.toFile(localFilePath)
            );

            Map<String, String> payload = Map.of("path", localFilePath.toString());

            log.info("Invoking Python sidecar for file: {}", localFilePath);
            IfcModelStats stats = restTemplate.postForObject(pythonUrl, payload, IfcModelStats.class);

            if (stats != null && "success".equalsIgnoreCase(stats.status())) {
                persistenceService.saveSuccessImport(
                        inputBucket,
                        s3Key,
                        stats
                );
                tx.setOutcome(Outcome.SUCCESS);

            } else {
                String errorMsg = stats != null ? stats.message() : "Null response from sidecar";
                throw new RuntimeException("Python processing failed: " + errorMsg);
            }

        } catch (Exception e) {
            tx.setOutcome(Outcome.FAILURE);
            tx.captureException(e);
            log.error("Critical error in task {}: {}", workId, e.getMessage(), e);
            throw new RuntimeException("IFC processing failed", e);
        } finally {
            tx.end();
            cleanup(workDir);
        }
    }

    private void cleanup(Path workDir) {
        if (workDir == null || !workDir.startsWith(sharedVolume)) {
            return;
        }
        try {
            if (Files.exists(workDir)) {
                try (var s = Files.walk(workDir)) {
                    s.sorted(Comparator.reverseOrder())
                            .forEach(path -> {
                                try {
                                    Files.delete(path);
                                } catch (IOException e) {
                                    log.warn("Failed to delete {}, it might be locked by another process: {}", path, e.getMessage());
                                }
                            });
                }
                log.info("Cleaned up workspace: {}", workDir);
            }
        } catch (IOException e) {
            log.error("Failed to traverse workspace for cleanup {}: {}", workDir, e.getMessage());
        }
    }

    private String extractS3Key(String messageJson) {
        S3EventNotification event = S3EventNotification.fromJson(messageJson);

        if (event.getRecords() == null || event.getRecords().isEmpty()) {
            throw new RuntimeException("SQS message contained no S3 Records");
        }

        String key = event.getRecords().get(0).getS3().getObject().getKey();
        return URLDecoder.decode(key, StandardCharsets.UTF_8);
    }

}