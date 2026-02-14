    package dk.trier.bim_stream_worker.persistence;


import jakarta.persistence.*;

@Entity
@Table(
        name = "model_element_count",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_import_element_type",
                columnNames = {"model_import_id", "elementType"}
        )
)
public class ModelElementCount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "model_import_id", nullable = false)
    private ModelImport modelImport;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private ElementType elementType;

    @Column(nullable = false)
    private int count;

    public Long getId() { return id; }

    public ModelImport getModelImport() { return modelImport; }
    public void setModelImport(ModelImport modelImport) { this.modelImport = modelImport; }

    public ElementType getElementType() { return elementType; }
    public void setElementType(ElementType elementType) { this.elementType = elementType; }

    public int getCount() { return count; }
    public void setCount(int count) { this.count = count; }
}
