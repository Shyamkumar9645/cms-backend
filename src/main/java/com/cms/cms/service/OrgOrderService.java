package com.cms.cms.service;


import com.cms.cms.model.Order;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public interface OrgOrderService {
    List<Order> getOrdersByOrgId(Integer orgId);
    Order getOrderById(Long orderId, Integer orgId);
    Order createOrder(Order order, Integer orgId);
    Order updateOrder(Order order, Integer orgId);
    boolean cancelOrder(Long orderId, Integer orgId);
}