package com.cms.cms.controller;

import com.cms.cms.Repository.NewOrgRepository;
import com.cms.cms.Repository.OrderRepository;
import com.cms.cms.model.NewOrg;
import com.cms.cms.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {
    private static final Logger logger = LoggerFactory.getLogger(AdminDashboardController.class);

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private NewOrgRepository organizationRepository;

    /**
     * Get dashboard summary data
     */
    @GetMapping("/summary")
    public ResponseEntity<?> getDashboardSummary() {
        logger.info("Fetching dashboard summary data");
        try {
            // Get all organizations count
            long totalUsers = organizationRepository.count();

            // Get all orders
            List<Order> allOrders = orderRepository.findAll();

            // Get total orders count
            long totalOrders = allOrders.size();

            // Calculate total sales
            BigDecimal totalSales = allOrders.stream()
                    .filter(order -> order.getTotalAmount() != null)
                    .map(Order::getTotalAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            // Count pending orders
            long pendingOrders = allOrders.stream()
                    .filter(order -> "Pending".equalsIgnoreCase(order.getStatus()) ||
                            "Processing".equalsIgnoreCase(order.getStatus()))
                    .count();

            // Calculate trends (simplified - in a real app you'd compare with previous periods)
            String userTrend = calculateTrend(totalUsers, 5.2);    // Example: 5.2% increase
            String orderTrend = calculateTrend(totalOrders, 3.8);  // Example: 3.8% increase
            String salesTrend = calculateTrend(totalSales.doubleValue(), -2.1); // Example: 2.1% decrease
            String pendingTrend = calculateTrend(pendingOrders, 1.5); // Example: 1.5% increase

            // Create response map
            Map<String, Object> summaryData = new HashMap<>();
            summaryData.put("totalUsers", totalUsers);
            summaryData.put("totalOrders", totalOrders);
            summaryData.put("totalSales", totalSales);
            summaryData.put("pendingOrders", pendingOrders);
            summaryData.put("userTrend", userTrend);
            summaryData.put("orderTrend", orderTrend);
            summaryData.put("salesTrend", salesTrend);
            summaryData.put("pendingTrend", pendingTrend);

            return ResponseEntity.ok(summaryData);

        } catch (Exception e) {
            logger.error("Error fetching dashboard summary", e);
            return ResponseEntity.internalServerError().body("Error fetching dashboard data: " + e.getMessage());
        }
    }

    /**
     * Get recent orders
     */
    @GetMapping("/recent-orders")
    public ResponseEntity<?> getRecentOrders() {
        logger.info("Fetching recent orders for dashboard");
        try {
            // Get all orders
            List<Order> allOrders = orderRepository.findAll();

            // Sort by date (most recent first) and take top 5
            List<Map<String, Object>> recentOrders = allOrders.stream()
                    .filter(order -> order.getOrgId() != null)
                    .sorted((o1, o2) -> {
                        // Sort by date if available
                        if (o1.getDate() != null && o2.getDate() != null) {
                            return o2.getDate().compareTo(o1.getDate());
                        }
                        // Fallback to ID
                        return o2.getId().compareTo(o1.getId());
                    })
                    .limit(5)
                    .map(order -> {
                        // Get organization name
                        Optional<NewOrg> org = organizationRepository.findById(Long.valueOf(order.getOrgId()));
                        String orgName = org.map(NewOrg::getOrganizationName).orElse("Unknown");

                        // Create order data with organization name
                        Map<String, Object> orderData = new HashMap<>();
                        orderData.put("id", order.getId());
                        orderData.put("orderId", order.getOrderId());
                        orderData.put("organizationName", orgName);
                        orderData.put("productName", order.getProductName());
                        orderData.put("date", order.getDate());
                        orderData.put("totalAmount", order.getTotalAmount());
                        orderData.put("status", order.getStatus());

                        return orderData;
                    })
                    .collect(Collectors.toList());

            return ResponseEntity.ok(recentOrders);

        } catch (Exception e) {
            logger.error("Error fetching recent orders", e);
            return ResponseEntity.internalServerError().body("Error fetching recent orders: " + e.getMessage());
        }
    }

    /**
     * Get sales data for chart
     */
    @GetMapping("/sales-data")
    public ResponseEntity<?> getSalesData() {
        logger.info("Fetching sales data for dashboard chart");
        try {
            // Get all orders
            List<Order> allOrders = orderRepository.findAll();

            // Group orders by month
            Map<String, BigDecimal> monthlySales = new HashMap<>();

            // Process each order
            for (Order order : allOrders) {
                if (order.getTotalAmount() == null) continue;

                // Extract month from date
                String month = extractMonthFromDate(order.getDate());

                // Add to monthly sales
                monthlySales.put(
                        month,
                        monthlySales.getOrDefault(month, BigDecimal.ZERO).add(order.getTotalAmount())
                );
            }

            // If no data, generate sample data
            if (monthlySales.isEmpty()) {
                return ResponseEntity.ok(generateSampleSalesData());
            }

            // Convert to chart format
            List<Map<String, Object>> chartData = new ArrayList<>();

            // Sort months
            List<String> sortedMonths = new ArrayList<>(monthlySales.keySet());
            Collections.sort(sortedMonths);

            // Create chart data points
            for (String month : sortedMonths) {
                Map<String, Object> dataPoint = new HashMap<>();
                dataPoint.put("name", month);
                dataPoint.put("value", monthlySales.get(month).intValue() / 1000); // Convert to thousands
                chartData.add(dataPoint);
            }

            return ResponseEntity.ok(chartData);

        } catch (Exception e) {
            logger.error("Error fetching sales data", e);
            return ResponseEntity.internalServerError().body("Error fetching sales data: " + e.getMessage());
        }
    }

    /**
     * Helper to extract month from date string
     */
    private String extractMonthFromDate(String dateStr) {
        try {
            // Try to parse the date string
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
            LocalDate date = LocalDate.parse(dateStr, formatter);
            return date.getMonth().toString().substring(0, 3);
        } catch (Exception e) {
            // If parsing fails, return current month
            return LocalDate.now().getMonth().toString().substring(0, 3);
        }
    }

    /**
     * Helper to calculate trend percentage
     */
    private String calculateTrend(double currentValue, double changePercent) {
        // In a real app, you would calculate this based on historical data
        // For now, we're using hardcoded example values
        String direction = changePercent >= 0 ? "Up" : "Down";
        return Math.abs(changePercent) + "% " + direction + " from yesterday";
    }

    /**
     * Generate sample sales data for chart when no real data exists
     */
    private List<Map<String, Object>> generateSampleSalesData() {
        List<Map<String, Object>> sampleData = new ArrayList<>();
        String[] months = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};
        Random random = new Random();

        for (String month : months) {
            Map<String, Object> dataPoint = new HashMap<>();
            dataPoint.put("name", month);
            dataPoint.put("value", 20 + random.nextInt(80)); // Random value between 20 and 100
            sampleData.add(dataPoint);
        }

        return sampleData;
    }

    /**
     * Generate sample orders for testing when the database is empty
     */
    private List<Order> generateSampleOrders(int count) {
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
            order.setOrgId(random.nextInt(5) + 1); // Random org ID between 1 and 5
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