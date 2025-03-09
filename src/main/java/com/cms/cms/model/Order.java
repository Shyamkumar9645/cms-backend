package com.cms.cms.model;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "orders")
@Data
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "organization_id", nullable = true)
    private Integer orgId;

    @Column(name = "status")
    private String status = "Pending";

    @Column(name = "total_amount")
    private BigDecimal totalAmount;

    @Column(name = "shipping_address")
    private String shippingAddress;

    @Column(name = "expected_delivery")
    private LocalDateTime expectedDelivery;

    @Column(name = "tracking_number")
    private String trackingNumber;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items;

    @Column(name = "order_id", nullable = false, unique = true)
    private String orderId;



    @PrePersist
    protected void onCreate() {
        if (orderId == null) {
            // Generate a unique ID - temporary implementation
            // In production, you'd use a more robust method
            orderId = "ORD-" + System.currentTimeMillis();

        }
    }


    // New fields from the form
    @Column(name = "prn_no")
    private String prnNo;

    @Column(name = "created_at")
    private String date;
    @Column(name = "product_name")
    private String productName;

    @Column(name = "brand")
    private String brand;

    @Column(name = "type")
    private String type;

    @Column(name = "unit_type")
    private String unitType;

    @Column(name = "batch_size_strips")
    private Integer batchSizeStrips;

    @Column(name = "unit")
    private String unit;

    @Column(name = "batch_size_tabs")
    private Integer batchSizeTabs;

    @Column(name = "mrp", precision = 10, scale = 2)
    private BigDecimal mrp;

    @Column(name = "size_code")
    private String sizeCode;

    @Column(name = "pvc_color")
    private String pvcColor;

    @Column(name = "packing_size")
    private String packingSize;

    @Column(name = "rate", precision = 10, scale = 2)
    private BigDecimal rate;

    @Column(name = "remarks")
    private String remarks;

    @Column(name = "cylinder_charges", precision = 10, scale = 2)
    private BigDecimal cylinderCharges;

    @Column(name = "dpco_mrp", precision = 10, scale = 2)
    private BigDecimal dpcoMrp;

    @Column(name = "composition")
    private String composition;

    @Column(name = "quantity")
    private Integer quantity;

    @Column(name = "price", precision = 10, scale = 2)
    private BigDecimal price;


}