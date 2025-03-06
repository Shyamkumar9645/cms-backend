package com.cms.cms.service;


import com.cms.cms.Repository.OrderRepository;
import com.cms.cms.model.Order;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class OrgOrderServiceImpl implements OrgOrderService {

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
        return orderRepository.save(order);
    }

    @Override
    public Order updateOrder(Order order, Integer orgId) {
        // Verify the order belongs to the organization
        if (!order.getOrgId().equals(orgId)) {
            throw new SecurityException("Not authorized to update this order");
        }
        return orderRepository.save(order);
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