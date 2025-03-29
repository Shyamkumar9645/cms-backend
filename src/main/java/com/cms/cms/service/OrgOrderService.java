package com.cms.cms.service;

import com.cms.cms.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public interface OrgOrderService {
    /**
     * Get all orders for an organization
     */
    List<Order> getOrdersByOrgId(Integer orgId);

    /**
     * Get paginated and filtered orders for an organization
     *
     * @param orgId The organization ID
     * @param filters Map of filter criteria
     *                (search, status, startDate, endDate, minPrice, maxPrice)
     * @param pageable Pagination and sorting information
     * @return Page of orders matching the criteria
     */
    Page<Order> getOrdersByOrgIdWithFilters(Integer orgId, Map<String, Object> filters, Pageable pageable);

    /**
     * Get a specific order by ID for an organization
     */
    Order getOrderById(Long orderId, Integer orgId);

    /**
     * Create a new order for an organization
     */
    Order createOrder(Order order, Integer orgId);

    /**
     * Update an existing order for an organization
     */
    Order updateOrder(Order order, Integer orgId);

    /**
     * Cancel an order for an organization
     */
    boolean cancelOrder(Long orderId, Integer orgId);

    /**
     * Get all pending orders across all organizations
     */
    List<Order> getAllPendingOrders();

    /**
     * Get the count of pending orders
     */
    long countPendingOrders();

    /**
     * Get a specific pending order by ID
     */
    Order getPendingOrderById(Long orderId);

    /**
     * Approve a pending order (change status to Processing)
     */
    Order approveOrder(Long orderId, Order orderDetails);

    /**
     * Reject a pending order
     */
    Order rejectOrder(Long orderId, String rejectionReason);
}