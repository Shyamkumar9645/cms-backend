// src/main/java/com/cms/cms/controller/AdminOrderController.java - Optimized version
package com.cms.cms.controller;

import com.cms.cms.model.Order;
import com.cms.cms.service.OrgOrderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminOrderController {
    private static final Logger logger = LoggerFactory.getLogger(AdminOrderController.class);

    @Autowired
    private OrgOrderService orgOrderService;

    @GetMapping("/company/{companyId}/orders")
    public ResponseEntity<?> getOrdersForCompany(@PathVariable Long companyId) {
        logger.info("Fetching orders for company ID: {}", companyId);
        try {
            if (companyId == null || companyId <= 0) {
                logger.warn("Invalid company ID: {}", companyId);
                return ResponseEntity.badRequest().body(createErrorResponse("Invalid company ID"));
            }

            List<Order> orders = orgOrderService.getOrdersByOrgId(Math.toIntExact(companyId));

            // Return empty array instead of 404 when no orders found
            if (orders == null || orders.isEmpty()) {
                logger.info("No orders found for company ID: {}", companyId);
                return ResponseEntity.ok(Collections.emptyList());
            }

            logger.info("Successfully retrieved {} orders for company ID: {}", orders.size(), companyId);
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            logger.error("Error getting orders for company ID: {}", companyId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("An error occurred: " + e.getMessage()));
        }
    }

    @GetMapping("/orders/{orgId}")
    public ResponseEntity<?> getOrdersForOrg(@PathVariable Long orgId) {
        logger.info("Fetching orders for organization ID: {}", orgId);
        try {
            if (orgId == null || orgId <= 0) {
                logger.warn("Invalid organization ID: {}", orgId);
                return ResponseEntity.badRequest().body(createErrorResponse("Invalid organization ID"));
            }

            List<Order> orders = orgOrderService.getOrdersByOrgId(Math.toIntExact(orgId));

            if (orders == null || orders.isEmpty()) {
                logger.info("No orders found for organization ID: {}", orgId);
                return ResponseEntity.ok(Collections.emptyList());
            }

            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            logger.error("Error getting orders for organization ID: {}", orgId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("An error occurred: " + e.getMessage()));
        }
    }

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

    @PutMapping("/orders/{orderId}")
    public ResponseEntity<?> updateOrder(@PathVariable Long orderId, @Valid @RequestBody Order order) {
        logger.info("Updating order ID: {}", orderId);
        try {
            // Ensure the path ID matches the order ID
            if (orderId == null || !orderId.equals(order.getId())) {
                logger.warn("Order ID mismatch: path ID {} vs body ID {}", orderId, order.getId());
                return ResponseEntity.badRequest().body(createErrorResponse("Order ID mismatch"));
            }

            // Make sure orgId is set
            if (order.getOrgId() == null) {
                logger.warn("Missing organization ID in order update");
                return ResponseEntity.badRequest().body(createErrorResponse("Organization ID is required"));
            }

            Order updatedOrder = orgOrderService.updateOrder(order, order.getOrgId());
            logger.info("Order updated successfully: {}", updatedOrder.getId());
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            logger.error("Error updating order ID: {}", orderId, e);
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