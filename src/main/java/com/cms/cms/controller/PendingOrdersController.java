package com.cms.cms.controller;

import com.cms.cms.model.Order;
import com.cms.cms.service.OrgOrderService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/orders")
public class PendingOrdersController {
    private static final Logger logger = LoggerFactory.getLogger(PendingOrdersController.class);

    @Autowired
    private OrgOrderService orgOrderService;

    /**
     * Get all pending orders
     */
    @GetMapping("/pending")
    public ResponseEntity<?> getAllPendingOrders() {
        logger.info("Fetching all pending orders");
        try {
            List<Order> pendingOrders = orgOrderService.getAllPendingOrders();
            return ResponseEntity.ok(pendingOrders);
        } catch (Exception e) {
            logger.error("Error fetching pending orders", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("An error occurred: " + e.getMessage()));
        }
    }

    /**
     * Get count of pending orders
     */
    @GetMapping("/pending/count")
    public ResponseEntity<?> getPendingOrdersCount() {
        logger.info("Fetching pending orders count");
        try {
            long count = orgOrderService.countPendingOrders();
            Map<String, Long> response = new HashMap<>();
            response.put("count", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error fetching pending orders count", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("An error occurred: " + e.getMessage()));
        }
    }

    /**
     * Get a specific pending order
     */
    @GetMapping("/pending/{orderId}")
    public ResponseEntity<?> getPendingOrderById(@PathVariable Long orderId) {
        logger.info("Fetching pending order with ID: {}", orderId);
        try {
            Order order = orgOrderService.getPendingOrderById(orderId);
            if (order == null) {
                logger.info("Pending order not found: {}", orderId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse("Pending order not found"));
            }
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            logger.error("Error fetching pending order: {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("An error occurred: " + e.getMessage()));
        }
    }

    /**
     * Approve a pending order
     */
    @PutMapping("/{orderId}/approve")
    public ResponseEntity<?> approveOrder(
            @PathVariable Long orderId,
            @Valid @RequestBody Order orderDetails) {
        logger.info("Approving order with ID: {}", orderId);
        try {
            // Validate required fields for approving an order
            if (orderDetails.getPrnNo() == null || orderDetails.getPrnNo().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("PRN Number is required"));
            }

            if (orderDetails.getBatchSizeStrips() == null || orderDetails.getBatchSizeStrips() <= 0) {
                return ResponseEntity.badRequest().body(createErrorResponse("Valid Batch Size (Strips) is required"));
            }

            if (orderDetails.getBatchSizeTabs() == null || orderDetails.getBatchSizeTabs() <= 0) {
                return ResponseEntity.badRequest().body(createErrorResponse("Valid Batch Size (Tabs) is required"));
            }

            if (orderDetails.getSizeCode() == null || orderDetails.getSizeCode().trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Size Code is required"));
            }

            // Approve the order
            Order approvedOrder = orgOrderService.approveOrder(orderId, orderDetails);
            return ResponseEntity.ok(approvedOrder);
        } catch (RuntimeException e) {
            logger.error("Error approving order: {}", orderId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error approving order: {}", orderId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("An error occurred: " + e.getMessage()));
        }
    }

    /**
     * Reject a pending order
     */
    @PutMapping("/{orderId}/reject")
    public ResponseEntity<?> rejectOrder(
            @PathVariable Long orderId,
            @RequestBody Map<String, String> requestBody) {
        logger.info("Rejecting order with ID: {}", orderId);
        try {
            String rejectionReason = requestBody.get("rejectionReason");

            if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(createErrorResponse("Rejection reason is required"));
            }

            Order rejectedOrder = orgOrderService.rejectOrder(orderId, rejectionReason);
            return ResponseEntity.ok(rejectedOrder);
        } catch (RuntimeException e) {
            logger.error("Error rejecting order: {}", orderId, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(createErrorResponse(e.getMessage()));
        } catch (Exception e) {
            logger.error("Error rejecting order: {}", orderId, e);
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