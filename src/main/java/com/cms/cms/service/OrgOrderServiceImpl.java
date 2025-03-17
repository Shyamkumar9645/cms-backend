package com.cms.cms.service;

import com.cms.cms.Repository.OrderRepository;
import com.cms.cms.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
    public Page<Order> getOrdersByOrgIdWithFilters(Integer orgId, Map<String, Object> filters, Pageable pageable) {
        logger.info("Fetching orders for organization {} with filters: {}", orgId, filters);

        try {
            // Get all orders for this organization
            List<Order> allOrders = getOrdersByOrgId(orgId);

            // Apply filters
            List<Order> filteredOrders = allOrders.stream()
                    .filter(order -> applyFilters(order, filters))
                    .collect(Collectors.toList());

            // Apply sorting and pagination
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), filteredOrders.size());

            // Paginate manually
            List<Order> pagedOrders = start >= filteredOrders.size() ?
                    new ArrayList<>() : filteredOrders.subList(start, end);

            logger.info("Returning {} orders from {} total filtered results for organization {}",
                    pagedOrders.size(), filteredOrders.size(), orgId);

            return new PageImpl<>(pagedOrders, pageable, filteredOrders.size());

        } catch (Exception e) {
            logger.error("Error fetching filtered orders for organization {}: {}", orgId, e.getMessage());
            throw e;
        }
    }

    /**
     * Apply filters to an order
     */
    private boolean applyFilters(Order order, Map<String, Object> filters) {
        // If no filters, return true
        if (filters == null || filters.isEmpty()) {
            return true;
        }

        // Search term filter
        if (filters.containsKey("search")) {
            String search = ((String) filters.get("search")).toLowerCase();

            boolean matchesSearch = false;

            // Check if search term matches any of these fields
            if (order.getProductName() != null && order.getProductName().toLowerCase().contains(search)) {
                matchesSearch = true;
            } else if (order.getBrand() != null && order.getBrand().toLowerCase().contains(search)) {
                matchesSearch = true;
            } else if (order.getOrderId() != null && order.getOrderId().toLowerCase().contains(search)) {
                matchesSearch = true;
            } else if (order.getId() != null && order.getId().toString().contains(search)) {
                matchesSearch = true;
            } else if (order.getPrnNo() != null && order.getPrnNo().toLowerCase().contains(search)) {
                matchesSearch = true;
            }

            if (!matchesSearch) {
                return false;
            }
        }

        // Status filter
        if (filters.containsKey("status")) {
            String status = (String) filters.get("status");
            if (order.getStatus() == null || !order.getStatus().equalsIgnoreCase(status)) {
                return false;
            }
        }

        // Date range filters
        if (filters.containsKey("startDate") || filters.containsKey("endDate")) {
            LocalDate orderDate = parseOrderDate(order.getDate());
            if (orderDate == null) {
                return false;
            }

            if (filters.containsKey("startDate")) {
                LocalDate startDate = (LocalDate) filters.get("startDate");
                if (orderDate.isBefore(startDate)) {
                    return false;
                }
            }

            if (filters.containsKey("endDate")) {
                LocalDate endDate = (LocalDate) filters.get("endDate");
                if (orderDate.isAfter(endDate)) {
                    return false;
                }
            }
        }

        // Price range filters
        if (filters.containsKey("minPrice") || filters.containsKey("maxPrice")) {
            BigDecimal orderAmount = order.getTotalAmount();
            if (orderAmount == null) {
                return false;
            }

            if (filters.containsKey("minPrice")) {
                BigDecimal minPrice = (BigDecimal) filters.get("minPrice");
                if (orderAmount.compareTo(minPrice) < 0) {
                    return false;
                }
            }

            if (filters.containsKey("maxPrice")) {
                BigDecimal maxPrice = (BigDecimal) filters.get("maxPrice");
                if (orderAmount.compareTo(maxPrice) > 0) {
                    return false;
                }
            }
        }

        // If all filters pass, return true
        return true;
    }

    /**
     * Parse order date string to LocalDate
     */
    private LocalDate parseOrderDate(String dateStr) {
        try {
            if (dateStr == null || dateStr.trim().isEmpty()) {
                return null;
            }

            // Common date formats
            String[] dateFormats = {
                    "dd MMM yyyy",
                    "yyyy-MM-dd",
                    "MM/dd/yyyy",
                    "dd/MM/yyyy"
            };

            // Try each format
            for (String format : dateFormats) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                    return LocalDate.parse(dateStr, formatter);
                } catch (Exception e) {
                    // Try next format
                }
            }

            // If no formats match, return null
            return null;

        } catch (Exception e) {
            logger.warn("Error parsing date '{}': {}", dateStr, e.getMessage());
            return null;
        }
    }

    @Override
    public Order getOrderById(Long orderId, Integer orgId) {
        Optional<Order> order = orderRepository.findByIdAndOrgId(orderId, orgId);
        return order.orElse(null);
    }

    @Override
    @Transactional
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