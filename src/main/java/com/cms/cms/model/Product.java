package com.cms.cms.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "products")
@Data
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "description")
    private String description;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable = true;

    @ElementCollection
    @CollectionTable(name = "product_unit_types", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "unit_type")
    private List<String> unitTypes;

    @ElementCollection
    @CollectionTable(name = "product_batch_sizes", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "batch_size")
    private List<String> availableBatches;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToMany(mappedBy = "products")
    private Set<NewOrg> organizations;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}