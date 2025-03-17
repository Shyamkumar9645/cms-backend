package com.cms.cms.controller;

import com.cms.cms.Repository.NewOrgRepository;
import com.cms.cms.Repository.OrderRepository;
import com.cms.cms.model.NewOrg;
import com.cms.cms.model.Order;
import com.cms.cms.service.OrganizationUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/org")
public class OrgDashboardController {
    private static final Logger logger = LoggerFactory.getLogger(OrgDashboardController.class);

    @Autowired
    private NewOrgRepository newOrgService;

    @Autowired
    private OrderRepository orderRepository;

    @PreAuthorize("hasRole('ORGANIZATION')")
    @GetMapping("/dashboard/profile")
    public ResponseEntity<?> getOrgProfile() {
        // Get current authenticated organization user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        OrganizationUserDetails userDetails = (OrganizationUserDetails) authentication.getPrincipal();

        // Get organization ID
        Integer orgId = userDetails.getOrgId();

        // Fetch organization details
        NewOrg orgDetails = newOrgService.findById(Long.valueOf(orgId))
                .orElse(null);

        if (orgDetails == null) {
            logger.warn("Organization not found for ID: {}", orgId);
            return ResponseEntity.notFound().build();
        }

        logger.info("Retrieved organization profile for ID: {}", orgId);
        return ResponseEntity.ok(orgDetails);
    }

    @PreAuthorize("hasRole('ORGANIZATION')")
    @GetMapping("/orders")
    public ResponseEntity<?> getOrgOrders() {
        // Get current authenticated organization user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        OrganizationUserDetails userDetails = (OrganizationUserDetails) authentication.getPrincipal();

        // Get organization ID
        Integer orgId = userDetails.getOrgId();

        // Fetch organization orders
        List<Order> orders = orderRepository.findByOrgId(orgId);

        // If no orders, generate sample data for testing
        if (orders == null || orders.isEmpty()) {
            logger.info("No orders found for organization ID: {}. Generating sample data for testing.", orgId);
            orders = generateSampleOrders(orgId, 10);
        }

        logger.info("Retrieved {} orders for organization ID: {}", orders.size(), orgId);
        return ResponseEntity.ok(orders);
    }

    @PreAuthorize("hasRole('ORGANIZATION')")
    @GetMapping("/dashboard/stats")
    public ResponseEntity<?> getOrgDashboardStats() {
        // Get current authenticated organization user
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        OrganizationUserDetails userDetails = (OrganizationUserDetails) authentication.getPrincipal();

        // Get organization ID
        Integer orgId = userDetails.getOrgId();

        // Fetch organization orders
        List<Order> orders = orderRepository.findByOrgId(orgId);

        // If no orders, generate sample data for testing
        if (orders == null || orders.isEmpty()) {
            orders = generateSampleOrders(orgId, 10);
        }

        // Calculate statistics
        Map<String, Object> stats = calculateStats(orders);

        logger.info("Generated dashboard stats for organization ID: {}", orgId);
        return ResponseEntity.ok(stats);
    }

    @PreAuthorize("hasRole('ORGANIZATION')")
    @GetMapping("/dashboard/orders")
    public ResponseEntity<?> getOrgDashboardOrders() {
        // Simply delegate to the existing method
        return getOrgOrders();
    }

    /**
     * Calculate statistics from orders
     */
    private Map<String, Object> calculateStats(List<Order> orders) {
        Map<String, Object> stats = new HashMap<>();

        // Calculate total orders
        int totalOrders = orders.size();
        stats.put("totalOrders", totalOrders);

        // Calculate pending orders
        long pendingOrders = orders.stream()
                .filter(order -> "Pending".equalsIgnoreCase(order.getStatus()) ||
                        "Processing".equalsIgnoreCase(order.getStatus()))
                .count();
        stats.put("pendingOrders", pendingOrders);

        // Calculate completed orders
        long completedOrders = orders.stream()
                .filter(order -> "Completed".equalsIgnoreCase(order.getStatus()) ||
                        "Delivered".equalsIgnoreCase(order.getStatus()))
                .count();
        stats.put("completedOrders", completedOrders);

        // Calculate total sales amount
        BigDecimal totalAmount = orders.stream()
                .filter(order -> order.getTotalAmount() != null)
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        stats.put("totalAmount", totalAmount);

        return stats;
    }

    /**
     * Generate sample orders for testing
     */
    private List<Order> generateSampleOrders(Integer orgId, int count) {
        List<Order> sampleOrders = new ArrayList<>();
        Random random = new Random();
        String[] productNames = {"Pain Relief Tablet", "Cough Syrup", "Antibiotic Capsule", "Vitamin Complex",
                "Blood Pressure Medicine", "Antacid", "Insulin", "Allergy Medication"};
        String[] brands = {"Pharma Plus", "MediLife", "HealthCare", "VitaWell", "MediCorp"};
        String[] statuses = {"Pending", "Processing", "Completed", "Shipped", "Delivered"};
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");

        for (int i = 0; i < count; i++) {
            Order order = new Order();
            order.setId((long) (i + 1));
            order.setOrderId("ORD-" + (1000 + i));
            order.setOrgId(orgId);
            order.setProductName(productNames[random.nextInt(productNames.length)]);
            order.setBrand(brands[random.nextInt(brands.length)]);
            order.setStatus(statuses[random.nextInt(statuses.length)]);

            // Set date to a recent date
            LocalDate date = LocalDate.now().minusDays(random.nextInt(60));
            order.setDate(date.format(formatter));

            // Set price and quantity
            BigDecimal price = BigDecimal.valueOf(100 + random.nextInt(900));
            int quantity = 10 + random.nextInt(90);
            order.setPrice(price);
            order.setQuantity(quantity);

            // Set total amount
            order.setTotalAmount(price.multiply(BigDecimal.valueOf(quantity)));

            sampleOrders.add(order);
        }

        return sampleOrders;
    }
}