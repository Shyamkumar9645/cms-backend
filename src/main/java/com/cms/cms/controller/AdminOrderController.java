package com.cms.cms.controller;

import com.cms.cms.model.Order;
import com.cms.cms.service.OrgOrderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminOrderController {
    private static final Logger logger = LoggerFactory.getLogger(AdminOrderController.class);

    @Autowired
    private OrgOrderService orgOrderService;

    /**
     * Get orders for company with pagination and filtering
     */
    @GetMapping("/company/{companyId}/orders")
    public ResponseEntity<?> getOrdersForCompany(
            @PathVariable Long companyId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "date") String sort,
            @RequestParam(defaultValue = "desc") String direction
    ) {
        logger.info("Fetching orders for company ID: {} with pagination and filtering", companyId);
        try {
            if (companyId == null || companyId <= 0) {
                logger.warn("Invalid company ID: {}", companyId);
                return ResponseEntity.badRequest().body(createErrorResponse("Invalid company ID"));
            }

            // Create Pageable with sorting
            Pageable pageable = PageRequest.of(
                    page,
                    size,
                    Sort.Direction.fromString(direction),
                    sort
            );

            // Create filter map
            Map<String, Object> filters = new HashMap<>();
            if (search != null && !search.trim().isEmpty()) {
                filters.put("search", search.trim());
            }
            if (status != null && !status.trim().isEmpty() && !status.equalsIgnoreCase("all")) {
                filters.put("status", status.trim());
            }
            if (startDate != null) {
                filters.put("startDate", startDate);
            }
            if (endDate != null) {
                filters.put("endDate", endDate);
            }
            if (minPrice != null) {
                filters.put("minPrice", minPrice);
            }
            if (maxPrice != null) {
                filters.put("maxPrice", maxPrice);
            }

            // Get paginated and filtered orders
            Page<Order> ordersPage = orgOrderService.getOrdersByOrgIdWithFilters(
                    Math.toIntExact(companyId),
                    filters,
                    pageable
            );

            logger.info("Successfully retrieved {} orders for company ID: {}",
                    ordersPage.getNumberOfElements(), companyId);

            return ResponseEntity.ok(ordersPage);
        } catch (Exception e) {
            logger.error("Error getting orders for company ID: {}", companyId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("An error occurred: " + e.getMessage()));
        }
    }

    // Get specific order details (existing method)
    @GetMapping("/orders/{orgId}/{orderId}")
    public ResponseEntity<?> getOrderDetails(@PathVariable Long orgId, @PathVariable Long orderId) {
        logger.info("Fetching order details for orderId: {}, orgId: {}", orderId, orgId);
        try {
            if (orgId == null || orgId <= 0 || orderId == null || orderId <= 0) {
                logger.warn("Invalid orgId or orderId: {} / {}", orgId, orderId);
                return ResponseEntity.badRequest().body(createErrorResponse("Invalid organization ID or order ID"));
            }

            Order order = orgOrderService.getOrderById(orderId, Math.toIntExact(orgId));
            if (order == null) {
                logger.info("Order not found: {} for orgId: {}", orderId, orgId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Order not found"));
            }
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            logger.error("Error getting order details for orderId: {}, orgId: {}", orderId, orgId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("An error occurred: " + e.getMessage()));
        }
    }

    // Submit new order (existing method)
    @PostMapping("/orders/submit")
    public ResponseEntity<?> submitOrders(@Valid @RequestBody Order order) {
        logger.info("Submitting new order for orgId: {}", order.getOrgId());
        try {
            // Make sure orgId is explicitly set in the payload
            if (order.getOrgId() == null) {
                logger.warn("Missing organization ID in order submission");
                return ResponseEntity.badRequest().body(createErrorResponse("Organization ID is required"));
            }

            // Validate additional required fields
            if (order.getProductName() == null || order.getProductName().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Product name is required"));
            }

            if (order.getQuantity() == null || order.getQuantity() <= 0) {
                return ResponseEntity.badRequest().body(createErrorResponse("Valid quantity is required"));
            }

            Order newOrder = orgOrderService.createOrder(order, order.getOrgId());
            logger.info("Order created successfully with ID: {}", newOrder.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(newOrder);
        } catch (Exception e) {
            logger.error("Error submitting order for orgId: {}", order.getOrgId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("An error occurred: " + e.getMessage()));
        }
    }

    // Update order (existing method)
    @PutMapping("/orders/{companyId}/{orderId}")
    public ResponseEntity<?> updateOrderWithCompanyId(
            @PathVariable Long companyId,
            @PathVariable Long orderId,
            @Valid @RequestBody Order order) {

        logger.info("Updating order ID: {} for company ID: {}", orderId, companyId);
        try {
            // Set IDs from path if not provided in the body
            if (order.getId() == null) {
                order.setId(orderId);
                logger.info("Setting order ID from path: {}", orderId);
            } else if (!orderId.equals(order.getId())) {
                logger.warn("Order ID mismatch: path ID {} vs body ID {}", orderId, order.getId());
                return ResponseEntity.badRequest().body(createErrorResponse("Order ID mismatch"));
            }

            if (order.getOrgId() == null) {
                order.setOrgId(Math.toIntExact(companyId));
                logger.info("Setting organization ID from path: {}", companyId);
            } else if (!companyId.equals(Long.valueOf(order.getOrgId()))) {
                logger.warn("Company ID mismatch: path ID {} vs body ID {}", companyId, order.getOrgId());
                return ResponseEntity.badRequest().body(createErrorResponse("Company ID mismatch"));
            }

            // Validate required fields
            if (order.getProductName() == null || order.getProductName().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Product name is required"));
            }

            if (order.getQuantity() == null) {
                return ResponseEntity.badRequest().body(createErrorResponse("Quantity is required"));
            }

            // Log the order data before updating
            logger.info("Updating order with data: id={}, orgId={}, status={}, productName={}, quantity={}",
                    order.getId(), order.getOrgId(), order.getStatus(),
                    order.getProductName(), order.getQuantity());

            // Use the service to update the order
            Order updatedOrder = orgOrderService.updateOrder(order, order.getOrgId());

            // Log the updated order details
            logger.info("Order updated successfully: id={}, status={}",
                    updatedOrder.getId(), updatedOrder.getStatus());

            // Return the complete updated order to the frontend
            return ResponseEntity.ok(updatedOrder);
        } catch (SecurityException e) {
            // Handle authorization errors specifically
            logger.error("Security exception updating order: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(createErrorResponse("Not authorized: " + e.getMessage()));
        } catch (Exception e) {
            logger.error("Error updating order ID: {} for company ID: {}", orderId, companyId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("An error occurred: " + e.getMessage()));
        }
    }

    // Helper method to create standardized error responses
    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("error", message);
        return response;
    }
}