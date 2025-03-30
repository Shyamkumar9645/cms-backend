package com.cms.cms.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class OrgOrderDTO {
    // Organization information
    private Integer orgId;
    private String organizationName;

    // Order items
    private List<OrderItemDTO> items;

    // Order details
    private BigDecimal totalAmount;
    private String status;
    private String shippingAddress;
    private String contactEmail;
    private String contactPhone;

    // DTO for order items with enhanced fields
    @Data
    public static class OrderItemDTO {
        private Long productId;
        private String productName;
        private String batchSize;
        private String unitType;
        private BigDecimal mrp;
        private Integer quantity;
        private BigDecimal subtotal;

        // Additional fields for more detailed product information
        private String brand;
        private String type;
        private String composition;
        private String sizeCode;
        private String pvcColor;
        private String packingSize;
        private String remarks;
    }
}