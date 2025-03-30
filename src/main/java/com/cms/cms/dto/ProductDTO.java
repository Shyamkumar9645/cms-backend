package com.cms.cms.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * Data Transfer Object for Product
 * Used for sending simplified product data to the frontend
 */
@Data
public class ProductDTO {
    private Long id;
    private String name;
    private String type;
    private String description;
    private BigDecimal price;
    private List<String> unitTypes;
    private List<String> availableBatches;
}