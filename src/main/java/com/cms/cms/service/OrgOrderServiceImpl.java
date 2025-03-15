package com.cms.cms.service;

import com.cms.cms.Repository.OrderRepository;
import com.cms.cms.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class OrgOrderServiceImpl implements OrgOrderService {
    private static final Logger logger = LoggerFactory.getLogger(OrgOrderServiceImpl.class);

    @Autowired
    private OrderRepository orderRepository;

    @Override
    public List<Order> getOrdersByOrgId(Integer orgId) {
        return orderRepository.findByOrgId(orgId);
    }

    @Override
    public Order getOrderById(Long orderId, Integer orgId) {
        Optional<Order> order = orderRepository.findByIdAndOrgId(orderId, orgId);
        return order.orElse(null);
    }

    @Override
    public Order createOrder(Order order, Integer orgId) {
        order.setOrgId(orgId);

        // Set default status if not provided
        if (order.getStatus() == null || order.getStatus().isEmpty()) {
            order.setStatus("Pending");
        }

        // Generate order ID if not set
        if (order.getOrderId() == null || order.getOrderId().isEmpty()) {
            order.setOrderId("ORD-" + System.currentTimeMillis());
        }

        return orderRepository.save(order);
    }

    @Override
    @Transactional
    public Order updateOrder(Order order, Integer orgId) {
        logger.info("Updating order {} for organization {}", order.getId(), orgId);

        // First fetch the current order to make sure it exists and belongs to the organization
        Optional<Order> existingOrderOpt = orderRepository.findByIdAndOrgId(order.getId(), orgId);

        if (existingOrderOpt.isEmpty()) {
            logger.error("Order {} not found for organization {}", order.getId(), orgId);
            throw new RuntimeException("Order not found or does not belong to this organization");
        }

        Order existingOrder = existingOrderOpt.get();

        // Verify the order belongs to the organization
        if (!existingOrder.getOrgId().equals(orgId)) {
            logger.error("Attempt to update order {} with mismatched organization ID: {} vs {}",
                    order.getId(), order.getOrgId(), orgId);
            throw new SecurityException("Not authorized to update this order: organization ID mismatch");
        }

        // Preserve the orderId from existing order
        if (order.getOrderId() == null || order.getOrderId().isEmpty()) {
            order.setOrderId(existingOrder.getOrderId());
        }

        // Ensure organization ID is correct
        order.setOrgId(orgId);

        // Validate and copy fields that need special handling

        // Handle status updates
        if (order.getStatus() == null || order.getStatus().isEmpty()) {
            order.setStatus(existingOrder.getStatus());
        }

        // Handle date fields - keep original if not provided
        if (order.getDate() == null || order.getDate().isEmpty()) {
            order.setDate(existingOrder.getDate());
        }

        // Log the fields being updated
        logger.info("Updating order with: productName={}, status={}, totalAmount={}, quantity={}",
                order.getProductName(), order.getStatus(), order.getTotalAmount(), order.getQuantity());

        // Save the updated order
        Order updatedOrder = orderRepository.save(order);
        logger.info("Successfully updated order {}", updatedOrder.getId());

        return updatedOrder;
    }

    @Override
    public boolean cancelOrder(Long orderId, Integer orgId) {
        Optional<Order> orderOptional = orderRepository.findByIdAndOrgId(orderId, orgId);

        if (orderOptional.isPresent()) {
            Order order = orderOptional.get();

            // Check if order can be canceled (e.g., not already shipped)
            if ("Pending".equals(order.getStatus()) || "Processing".equals(order.getStatus())) {
                order.setStatus("Cancelled");
                orderRepository.save(order);
                return true;
            }
        }

        return false;
    }
}