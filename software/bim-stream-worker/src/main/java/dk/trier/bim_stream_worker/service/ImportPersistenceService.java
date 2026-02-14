package dk.trier.bim_stream_worker.service;

import dk.trier.bim_stream_worker.persistence.IfcModelStats;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import dk.trier.bim_stream_worker.persistence.ElementType;
import dk.trier.bim_stream_worker.persistence.ModelElementCount;
import dk.trier.bim_stream_worker.persistence.ModelImport;
import dk.trier.bim_stream_worker.persistence.ModelImportRepository;

import java.util.Map;

@Service
public class ImportPersistenceService {
    private static final Logger log = LoggerFactory.getLogger(ImportPersistenceService.class);

    private final ModelImportRepository importRepo;

    public ImportPersistenceService(ModelImportRepository importRepo) {
        this.importRepo = importRepo;
    }

    @Transactional
    public ModelImport saveSuccessImport(
            String sourceBucket,
            String sourceKey,
            IfcModelStats stats
    ) {
        ModelImport imp = new ModelImport();
        imp.setSourceBucket(sourceBucket);
        imp.setSourceKey(sourceKey);

        if (stats != null) {
            imp.setIfcSchema(stats.ifcSchema());
            imp.setSourceApplication(stats.sourceApp());
            imp.setProjectName(stats.projectName());
            imp.setSiteName(stats.siteName());
            imp.setBuildingName(stats.buildingName());

            Map<String, Integer> counts = stats.counts();
            if (counts != null) {
                stats.counts().forEach((type, count) -> {
                    String normalized = type == null ? "" : type.trim().toUpperCase();
                    try {
                        ElementType elementType = ElementType.valueOf(normalized);

                        ModelElementCount c = new ModelElementCount();
                        c.setElementType(elementType);
                        c.setCount(count == null ? 0 : count);
                        imp.addCount(c);

                    } catch (IllegalArgumentException e) {
                        log.warn("Ignoring unsupported element type from Python: {}", type);
                    }
                });
            }
        }
        return importRepo.save(imp);
    }


}