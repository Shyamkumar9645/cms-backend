package com.cms.cms.controller;

import com.cms.cms.model.Order;
import com.cms.cms.service.OrgOrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class AdminOrderController {

    @Autowired
    private OrgOrderService orgOrderService;

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/orders/submit")
    public ResponseEntity<?> submitOrders(@Valid @RequestBody Order order) {
        try {
            Order newOrder = orgOrderService.createOrder(order, order.getOrgId());
            return new ResponseEntity<>(newOrder, HttpStatus.CREATED);  // Return 201 Created
        } catch (Exception e) {
            return new ResponseEntity<>("An error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR); // 500
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

            // You'll need to implement this method in your OrgOrderService
            Order updatedOrder = orgOrderService.updateOrder(order, order.getOrgId());
            return ResponseEntity.ok(updatedOrder);
        } catch (Exception e) {
            return new ResponseEntity<>("An error occurred: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


}