package com.cms.cms.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.HashSet;

@Entity
@Table(name = "products")
@Getter
@Setter
@ToString(exclude = "organizations") // Exclude organizations from toString
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
    @JsonIgnoreProperties("products")
    private Set<NewOrg> organizations = new HashSet<>();

    // Custom equals that doesn't use the organizations collection
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Product product = (Product) o;
        return id != null && Objects.equals(id, product.id);
    }

    // Custom hashCode that doesn't use the organizations collection
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}