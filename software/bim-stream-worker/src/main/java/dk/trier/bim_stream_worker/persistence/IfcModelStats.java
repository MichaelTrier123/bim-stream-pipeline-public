package dk.trier.bim_stream_worker.persistence;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record IfcModelStats(
        String status,
        String message,
        String ifcSchema,
        String sourceApp,
        String projectName,
        String siteName,
        String buildingName,
        Map<String, Integer> counts
) {}