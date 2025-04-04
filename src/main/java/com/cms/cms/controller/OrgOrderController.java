package com.cms.cms.controller;

import com.cms.cms.Repository.OrderRepository;
import com.cms.cms.Repository.NewOrgRepository;
import com.cms.cms.dto.OrgOrderDTO;
import com.cms.cms.dto.ProductDTO;
import com.cms.cms.model.NewOrg;
import com.cms.cms.model.Order;
import com.cms.cms.model.OrderItem;
import com.cms.cms.model.Product;
import com.cms.cms.service.OrgOrderService;
import com.cms.cms.service.OrganizationUserDetails;
import com.cms.cms.service.ProductService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/org")
public class OrgOrderController {
    private static final Logger logger = LoggerFactory.getLogger(OrgOrderController.class);

    @Autowired
    private ProductService productService;

    @Autowired
    private OrgOrderService orgOrderService;

    @Autowired
    private NewOrgRepository organizationRepository;

    /**
     * Get all available products
     * This endpoint returns a list of products that are available for ordering
     */
    @PreAuthorize("hasRole('ORGANIZATION')")
    @GetMapping("/available-products")
    public ResponseEntity<?> getAvailableProducts() {
        try {
            logger.info("Fetching available products for current organization");

            // Get current authenticated organization
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            OrganizationUserDetails userDetails = (OrganizationUserDetails) authentication.getPrincipal();
            Integer orgId = userDetails.getOrgId();

            logger.info("Retrieving products for organization ID: {}", orgId);

            // Get products assigned to this organization
            List<Product> availableProducts = productService.getProductsForOrganization(Long.valueOf(orgId));

            if (availableProducts.isEmpty()) {
                logger.info("No products found for organization ID: {}", orgId);
                return ResponseEntity.ok(Collections.emptyList());
            }

            // Convert Products to DTOs
            List<ProductDTO> productDTOs = availableProducts.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            logger.info("Returning {} available products for organization ID: {}", productDTOs.size(), orgId);
            return ResponseEntity.ok(productDTOs);

        } catch (Exception e) {
            logger.error("Error fetching available products", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error fetching available products: " + e.getMessage()));
        }
    }
    /**
     * Submit a new order
     * This endpoint allows organizations to submit new orders with multiple items
     */
    @PreAuthorize("hasRole('ORGANIZATION')")
    @PostMapping("/orders/submit")
    public ResponseEntity<?> submitOrder(@Valid @RequestBody OrgOrderDTO orderDTO) {
        try {
            logger.info("Received order submission request");

            // Get current authenticated organization
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            OrganizationUserDetails userDetails = (OrganizationUserDetails) authentication.getPrincipal();
            Integer orgId = userDetails.getOrgId();

            // Verify the organization ID in the request matches the authenticated user
            if (orderDTO.getOrgId() != null && !orderDTO.getOrgId().equals(orgId)) {
                logger.warn("Organization ID mismatch: {} vs {}", orderDTO.getOrgId(), orgId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(createErrorResponse("Not authorized to submit orders for other organizations"));
            }

            // If orgId is not set in DTO, set it from authenticated user
            if (orderDTO.getOrgId() == null) {
                orderDTO.setOrgId(orgId);
            }

            // Validate items in the order
            if (orderDTO.getItems() == null || orderDTO.getItems().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(createErrorResponse("Order must contain at least one item"));
            }

            // Create a new Order from the DTO
            Order order = new Order();
            order.setOrgId(orgId);
            order.setStatus("Pending");
            order.setTotalAmount(orderDTO.getTotalAmount());
            order.setShippingAddress(orderDTO.getShippingAddress());

            // Set order date to current date
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
            order.setDate(LocalDateTime.now().format(formatter));

            // Handle multiple items in the order
            if (orderDTO.getItems().size() == 1) {
                // Single item order - set properties directly on the order
                OrgOrderDTO.OrderItemDTO item = orderDTO.getItems().get(0);
                order.setProductName(item.getProductName());
                order.setBatchSize(item.getBatchSize());
                order.setUnitType(item.getUnitType());
                order.setQuantity(item.getQuantity());
                order.setPrice(item.getMrp());

                // Calculate total amount if not provided
                if (order.getTotalAmount() == null && item.getMrp() != null && item.getQuantity() != null) {
                    BigDecimal totalAmount = item.getMrp().multiply(new BigDecimal(item.getQuantity()));
                    order.setTotalAmount(totalAmount);
                }
            } else {
                // Multiple items - create a summary product name and set the first item's details
                StringBuilder productNameBuilder = new StringBuilder();
                for (int i = 0; i < orderDTO.getItems().size(); i++) {
                    OrgOrderDTO.OrderItemDTO item = orderDTO.getItems().get(i);
                    if (i > 0) productNameBuilder.append(", ");
                    productNameBuilder.append(item.getProductName());

                    // For brevity, limit to 3 product names
                    if (i == 2 && orderDTO.getItems().size() > 3) {
                        productNameBuilder.append(", and ").append(orderDTO.getItems().size() - 3).append(" more");
                        break;
                    }
                }

                // Set the summary product name
                order.setProductName(productNameBuilder.toString());

                // Set other details from the first item (can be customized as needed)
                OrgOrderDTO.OrderItemDTO firstItem = orderDTO.getItems().get(0);
                order.setBatchSize(firstItem.getBatchSize());
                order.setUnitType(firstItem.getUnitType());
                order.setQuantity(orderDTO.getItems().stream().mapToInt(OrgOrderDTO.OrderItemDTO::getQuantity).sum());

                // For price, use the average if needed
                BigDecimal totalPrice = orderDTO.getItems().stream()
                        .map(OrgOrderDTO.OrderItemDTO::getMrp)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);
                order.setPrice(totalPrice.divide(new BigDecimal(orderDTO.getItems().size()), BigDecimal.ROUND_HALF_UP));

                // Set up order items if your Order model supports this relationship
                List<OrderItem> orderItems = new ArrayList<>();
                for (OrgOrderDTO.OrderItemDTO itemDTO : orderDTO.getItems()) {
                    OrderItem orderItem = new OrderItem();
                    orderItem.setProductName(itemDTO.getProductName());
                    orderItem.setQuantity(itemDTO.getQuantity());
                    orderItem.setUnitPrice(itemDTO.getMrp());

                    // Add description or other fields if needed
                    String description = "Batch Size: " + itemDTO.getBatchSize() +
                            ", Unit Type: " + itemDTO.getUnitType();
                    orderItem.setProductDescription(description);

                    // Set reference to parent order (will be set after order is created)
                    // orderItem.setOrder(order);

                    orderItems.add(orderItem);
                }

                // If your order entity supports items, uncomment this
                // order.setItems(orderItems);
            }

            // Create and save the order
            Order createdOrder = orgOrderService.createOrder(order, orgId);

            // If you need to save related order items after the order is created:
            // if (orderDTO.getItems().size() > 1) {
            //     // Save order items with reference to the created order
            //     for (OrderItem item : order.getItems()) {
            //         item.setOrder(createdOrder);
            //     }
            //     // Save order items through a repository or service
            // }

            logger.info("Order created successfully with ID: {}", createdOrder.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(createdOrder);

        } catch (Exception e) {
            logger.error("Error submitting order", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error submitting order: " + e.getMessage()));
        }
    }

    /**
     * Convert Product entity to ProductDTO
     */
    private ProductDTO convertToDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setType(product.getType());
        dto.setDescription(product.getDescription());
        dto.setPrice(product.getPrice());
        dto.setUnitTypes(product.getUnitTypes());
        dto.setAvailableBatches(product.getAvailableBatches());
        return dto;
    }

    /**
     * Create sample products for demo purposes
     */
    private List<ProductDTO> createSampleProducts() {
        List<ProductDTO> sampleProducts = new ArrayList<>();

        // Create sample product 1
        ProductDTO product1 = new ProductDTO();
        product1.setId(1L);
        product1.setName("Paracetamol 500mg");
        product1.setType("Medicine");
        product1.setDescription("Pain reliever and fever reducer");
        product1.setPrice(new BigDecimal("150.00"));
        product1.setUnitTypes(List.of("I", "II"));
        product1.setAvailableBatches(List.of("100", "500", "1000"));
        sampleProducts.add(product1);

        // Create sample product 2
        ProductDTO product2 = new ProductDTO();
        product2.setId(2L);
        product2.setName("Amoxicillin 250mg");
        product2.setType("Medicine");
        product2.setDescription("Antibiotic for bacterial infections");
        product2.setPrice(new BigDecimal("280.00"));
        product2.setUnitTypes(List.of("I", "II"));
        product2.setAvailableBatches(List.of("50", "100", "500"));
        sampleProducts.add(product2);

        // Create sample product 3
        ProductDTO product3 = new ProductDTO();
        product3.setId(3L);
        product3.setName("Vitamin C 1000mg");
        product3.setType("Supplement");
        product3.setDescription("Immune system booster");
        product3.setPrice(new BigDecimal("350.00"));
        product3.setUnitTypes(List.of("I"));
        product3.setAvailableBatches(List.of("30", "60", "90"));
        sampleProducts.add(product3);

        // Create sample product 4
        ProductDTO product4 = new ProductDTO();
        product4.setId(4L);
        product4.setName("Ibuprofen 400mg");
        product4.setType("Medicine");
        product4.setDescription("Non-steroidal anti-inflammatory drug");
        product4.setPrice(new BigDecimal("200.00"));
        product4.setUnitTypes(List.of("I", "II"));
        product4.setAvailableBatches(List.of("100", "200", "500"));
        sampleProducts.add(product4);

        // Create sample product 5
        ProductDTO product5 = new ProductDTO();
        product5.setId(5L);
        product5.setName("Calcium + D3");
        product5.setType("Supplement");
        product5.setDescription("Bone health supplement");
        product5.setPrice(new BigDecimal("420.00"));
        product5.setUnitTypes(List.of("I"));
        product5.setAvailableBatches(List.of("60", "120"));
        sampleProducts.add(product5);

        return sampleProducts;
    }

    /**
     * Create standardized error response
     */
    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("error", message);
        return response;
    }
    @PreAuthorize("hasRole('ORGANIZATION')")
    @GetMapping("/orders/{orderId}")
    public ResponseEntity<?> getOrderDetails(@PathVariable Long orderId) {
        try {
            logger.info("Fetching order details for orderId: {}", orderId);

            // Get current authenticated organization
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            OrganizationUserDetails userDetails = (OrganizationUserDetails) authentication.getPrincipal();
            Integer orgId = userDetails.getOrgId();

            // Get the order from the service
            Order order = orgOrderService.getOrderById(orderId, orgId);

            if (order == null) {
                logger.warn("Order not found or does not belong to this organization: {}", orderId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Order not found"));
            }

            // Enhance the response with additional information if needed
            Map<String, Object> response = new HashMap<>();

            // Copy order properties
            response.put("id", order.getId());
            response.put("orderId", order.getOrderId());
            response.put("status", order.getStatus());
            response.put("date", order.getDate());
            response.put("expectedDelivery", order.getExpectedDelivery());
            response.put("totalAmount", order.getTotalAmount());
            response.put("shippingAddress", order.getShippingAddress());
            response.put("trackingNumber", order.getTrackingNumber());

            // Add product details
            response.put("productName", order.getProductName());
            response.put("brand", order.getBrand());
            response.put("type", order.getType());
            response.put("quantity", order.getQuantity());
            response.put("price", order.getPrice());

            // Add batch and unit information
            response.put("batchSize", order.getBatchSize());
            response.put("unitType", order.getUnitType());
            response.put("batchSizeStrips", order.getBatchSizeStrips());
            response.put("batchSizeTabs", order.getBatchSizeTabs());
            response.put("mrp", order.getMrp());

            // Add other order details
            response.put("sizeCode", order.getSizeCode());
            response.put("pvcColor", order.getPvcColor());
            response.put("packingSize", order.getPackingSize());
            response.put("remarks", order.getRemarks());

            // Include order items if they exist
            if (order.getItems() != null && !order.getItems().isEmpty()) {
                List<Map<String, Object>> itemsList = new ArrayList<>();

                for (OrderItem item : order.getItems()) {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("id", item.getId());
                    itemMap.put("productName", item.getProductName());
                    itemMap.put("productDescription", item.getProductDescription());
                    itemMap.put("unitPrice", item.getUnitPrice());
                    itemMap.put("quantity", item.getQuantity());
                    itemMap.put("subtotal", item.getUnitPrice().multiply(new BigDecimal(item.getQuantity())));

                    itemsList.add(itemMap);
                }

                response.put("items", itemsList);
            }

            // Include organization information
            try {
                NewOrg org = organizationRepository.findById(Long.valueOf(orgId)).orElse(null);
                if (org != null) {
                    Map<String, Object> orgInfo = new HashMap<>();
                    orgInfo.put("id", org.getId());
                    orgInfo.put("name", org.getOrganizationName());
                    orgInfo.put("address", org.getAddressLine1());
                    orgInfo.put("city", org.getCity());
                    orgInfo.put("zip", org.getZip());

                    response.put("organization", orgInfo);
                }
            } catch (Exception e) {
                logger.warn("Could not fetch organization details: {}", e.getMessage());
            }

            logger.info("Successfully retrieved order details for orderId: {}", orderId);
            return ResponseEntity.ok(response);

        } catch (SecurityException e) {
            logger.error("Security exception fetching order: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("Not authorized to view this order"));
        } catch (Exception e) {
            logger.error("Error fetching order details for orderId: {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("Error retrieving order details: " + e.getMessage()));
        }
    }
}