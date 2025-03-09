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
import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminOrderController {
    private static final Logger logger = LoggerFactory.getLogger(AdminOrderController.class);


    @Autowired
    private OrgOrderService orgOrderService;

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/company/{companyId}/orders")
    public ResponseEntity<?> getOrdersForCompany(@PathVariable Long companyId) {
        logger.info("Fetching orders for company ID: {}", companyId);
        try {
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
            return new ResponseEntity<>("An error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/orders/{orgId}")
    public ResponseEntity<?> getOrdersForOrg(@PathVariable Long orgId) {
        try {
            List<Order> orders = orgOrderService.getOrdersByOrgId(Math.toIntExact(orgId));
            return ResponseEntity.ok(orders);
        } catch (Exception e) {
            return new ResponseEntity<>("An error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/orders/{orgId}/{orderId}")
    public ResponseEntity<?> getOrderDetails(@PathVariable Long orgId, @PathVariable Long orderId) {
        try {
            Order order = orgOrderService.getOrderById(orderId, Math.toIntExact(orgId));
            if (order == null) {
                return new ResponseEntity<>("Order not found", HttpStatus.NOT_FOUND);
            }
            return ResponseEntity.ok(order);
        } catch (Exception e) {
            return new ResponseEntity<>("An error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/orders/submit")
    public ResponseEntity<?> submitOrders(@Valid @RequestBody Order order) {
        try {
            // Make sure orgId is explicitly set in the payload
            if (order.getOrgId() == null) {
                return new ResponseEntity<>("Organization ID is required", HttpStatus.BAD_REQUEST);
            }

            Order newOrder = orgOrderService.createOrder(order, order.getOrgId());
            return new ResponseEntity<>(newOrder, HttpStatus.CREATED);
        } catch (Exception e) {
            return new ResponseEntity<>("An error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/orders/{orderId}")
    public ResponseEntity<?> updateOrder(@PathVariable Long orderId, @Valid @RequestBody Order order) {
        try {
            // Ensure the path ID matches the order ID
            if (!orderId.equals(order.getId())) {
                return new ResponseEntity<>("Order ID mismatch", HttpStatus.BAD_REQUEST);
            }

            Order updatedOrder = orgOrderService.updateOrder(order, order.getOrgId());
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            return new ResponseEntity<>("An error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


}