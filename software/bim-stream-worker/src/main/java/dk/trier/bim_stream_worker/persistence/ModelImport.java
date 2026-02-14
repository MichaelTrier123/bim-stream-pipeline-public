package dk.trier.bim_stream_worker.persistence;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "model_import",
        indexes = {
                @Index(name = "idx_model_import_source", columnList = "sourceBucket, sourceKey")
        }
)
public class ModelImport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private String sourceBucket;

    @Column(nullable = false)
    private String sourceKey;

    private String ifcSchema;
    private String sourceApplication;

    private String projectName;
    private String siteName;
    private String buildingName;


    @OneToMany(mappedBy = "modelImport", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ModelElementCount> elementCounts = new ArrayList<>();

    public void addCount(ModelElementCount c) {
        c.setModelImport(this);
        this.elementCounts.add(c);
    }

    public Long getId() { return id; }
    public Instant getCreatedAt() { return createdAt; }

    public String getSourceBucket() { return sourceBucket; }
    public void setSourceBucket(String sourceBucket) { this.sourceBucket = sourceBucket; }

    public String getSourceKey() { return sourceKey; }
    public void setSourceKey(String sourceKey) { this.sourceKey = sourceKey; }

    public String getIfcSchema() { return ifcSchema; }
    public void setIfcSchema(String ifcSchema) { this.ifcSchema = ifcSchema; }

    public String getSourceApplication() { return sourceApplication; }
    public void setSourceApplication(String sourceApplication) { this.sourceApplication = sourceApplication; }

    public String getProjectName() { return projectName; }
    public void setProjectName(String projectName) { this.projectName = projectName; }

    public String getSiteName() { return siteName; }
    public void setSiteName(String siteName) { this.siteName = siteName; }

    public String getBuildingName() { return buildingName; }
    public void setBuildingName(String buildingName) { this.buildingName = buildingName; }


    public List<ModelElementCount> getElementCounts() {
        return elementCounts;
    }
}