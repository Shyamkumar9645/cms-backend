package com.cms.cms.controller;

import com.cms.cms.Repository.NewOrgRepository;
import com.cms.cms.Repository.OrderRepository;
import com.cms.cms.model.NewOrg;
import com.cms.cms.model.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.WeekFields;
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
     * Get sales data for chart with support for different time periods
     * @param period The time period: daily, weekly, monthly, or yearly (default: monthly)
     */
    @GetMapping("/sales-data")
    public ResponseEntity<?> getSalesData(@RequestParam(required = false, defaultValue = "monthly") String period) {
        logger.info("Fetching sales data for dashboard chart with period: {}", period);
        try {
            // Get all orders
            List<Order> allOrders = orderRepository.findAll();
            logger.info("Total orders found: {}", allOrders.size());

            // Log a sample order if available
            if (!allOrders.isEmpty()) {
                Order sampleOrder = allOrders.get(0);
                logger.info("Sample order - ID: {}, Date: {}, Amount: {}",
                        sampleOrder.getId(), sampleOrder.getDate(), sampleOrder.getTotalAmount());
            }

            // Filter out orders with null totalAmount
            List<Order> validOrders = allOrders.stream()
                    .filter(order -> order.getTotalAmount() != null)
                    .collect(Collectors.toList());

            logger.info("Orders with valid amounts: {}", validOrders.size());

            if (validOrders.isEmpty()) {
                logger.info("No valid orders found for sales data");
                return ResponseEntity.ok(new ArrayList<>());
            }

            // Process based on period
            List<Map<String, Object>> chartData;
            switch (period.toLowerCase()) {
                case "daily":
                    chartData = getDailySalesData(validOrders);
                    break;
                case "weekly":
                    chartData = getWeeklySalesData(validOrders);
                    break;
                case "yearly":
                    chartData = getYearlySalesData(validOrders);
                    break;
                case "monthly":
                default:
                    chartData = getMonthlySalesData(validOrders);
                    break;
            }

            logger.info("Returning {} data points for period: {}", chartData.size(), period);
            return ResponseEntity.ok(chartData);

        } catch (Exception e) {
            logger.error("Error fetching sales data", e);
            return ResponseEntity.internalServerError().body("Error fetching sales data: " + e.getMessage());
        }
    }

    /**
     * Parse date string from database format
     * Handles both formats: "2025-03-20 22:38:18.752437" and "dd MMM yyyy"
     */
    private LocalDate parseOrderDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }

        try {
            // Try to parse as ISO format (2025-03-20 22:38:18.752437)
            if (dateStr.contains("-")) {
                // Extract just the date part if there's a time component
                String datePart = dateStr.split(" ")[0];
                return LocalDate.parse(datePart);
            }
            // Try to parse as "dd MMM yyyy" format
            else {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
                return LocalDate.parse(dateStr, formatter);
            }
        } catch (Exception e) {
            logger.warn("Could not parse date: {} - {}", dateStr, e.getMessage());
            return null;
        }
    }

    /**
     * Get daily sales data for the past 7 days
     */
    private List<Map<String, Object>> getDailySalesData(List<Order> orders) {
        logger.info("Processing daily sales data");

        // Calculate the date range (last 7 days)
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusDays(6); // 7 days including today

        // Map to store daily sales
        Map<LocalDate, BigDecimal> dailySales = new TreeMap<>();

        // Initialize all dates in range with zero
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
            dailySales.put(date, BigDecimal.ZERO);
        }

        // Count orders with valid dates
        int validOrderCount = 0;

        // Aggregate sales by day
        for (Order order : orders) {
            LocalDate orderDate = parseOrderDate(order.getDate());

            if (orderDate != null) {
                validOrderCount++;

                // Only include orders in our date range
                if (!orderDate.isBefore(startDate) && !orderDate.isAfter(endDate)) {
                    dailySales.put(
                            orderDate,
                            dailySales.getOrDefault(orderDate, BigDecimal.ZERO).add(order.getTotalAmount())
                    );
                }
            }
        }

        logger.info("Found {} orders with valid dates out of {} total for daily view", validOrderCount, orders.size());

        // Convert to chart format
        List<Map<String, Object>> chartData = new ArrayList<>();
        DateTimeFormatter outputFormatter = DateTimeFormatter.ofPattern("EEE, MMM d"); // e.g., "Mon, Jan 1"

        for (Map.Entry<LocalDate, BigDecimal> entry : dailySales.entrySet()) {
            Map<String, Object> dataPoint = new HashMap<>();
            dataPoint.put("name", entry.getKey().format(outputFormatter));
            dataPoint.put("value", entry.getValue().intValue()); // Use actual value
            chartData.add(dataPoint);
        }

        return chartData;
    }

    /**
     * Get weekly sales data for the past 4-5 weeks
     */
    private List<Map<String, Object>> getWeeklySalesData(List<Order> orders) {
        logger.info("Processing weekly sales data");

        // Calculate the date range (last 4 weeks)
        LocalDate endDate = LocalDate.now();
        LocalDate startDate = endDate.minusWeeks(4);

        // Map to store weekly sales (key: year-week)
        Map<String, BigDecimal> weeklySales = new TreeMap<>();

        // Initialize all weeks in range with zero
        for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusWeeks(1)) {
            int year = date.getYear();
            int weekOfYear = date.get(WeekFields.ISO.weekOfYear());
            String weekKey = String.format("%d-W%02d", year, weekOfYear);
            weeklySales.put(weekKey, BigDecimal.ZERO);
        }

        // Count orders with valid dates
        int validOrderCount = 0;

        // Aggregate sales by week
        for (Order order : orders) {
            LocalDate orderDate = parseOrderDate(order.getDate());

            if (orderDate != null) {
                validOrderCount++;

                // Only include orders in reasonable time frame (last 6 months)
                if (orderDate.isAfter(endDate.minusMonths(6))) {
                    int year = orderDate.getYear();
                    int weekOfYear = orderDate.get(WeekFields.ISO.weekOfYear());
                    String weekKey = String.format("%d-W%02d", year, weekOfYear);

                    // Only add to existing weeks in our map (last 4 weeks)
                    if (weeklySales.containsKey(weekKey)) {
                        weeklySales.put(
                                weekKey,
                                weeklySales.get(weekKey).add(order.getTotalAmount())
                        );
                    }
                }
            }
        }

        logger.info("Found {} orders with valid dates out of {} total for weekly view", validOrderCount, orders.size());

        // Convert to chart format
        List<Map<String, Object>> chartData = new ArrayList<>();

        for (Map.Entry<String, BigDecimal> entry : weeklySales.entrySet()) {
            Map<String, Object> dataPoint = new HashMap<>();
            dataPoint.put("name", entry.getKey().replace("-", " ")); // Format: "2023 W01"
            dataPoint.put("value", entry.getValue().intValue()); // Use actual value
            chartData.add(dataPoint);
        }

        return chartData;
    }

    /**
     * Get monthly sales data for the current year
     */
    private List<Map<String, Object>> getMonthlySalesData(List<Order> orders) {
        logger.info("Processing monthly sales data");

        // Map to store monthly sales
        Map<String, BigDecimal> monthlySales = new TreeMap<>();

        // Initialize all months with zero (for current year)
        int currentYear = LocalDate.now().getYear();
        String[] monthAbbreviations = {"Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"};

        for (String month : monthAbbreviations) {
            monthlySales.put(month, BigDecimal.ZERO);
        }

        // Count orders with valid dates
        int validOrderCount = 0;

        // Aggregate sales by month
        for (Order order : orders) {
            LocalDate orderDate = parseOrderDate(order.getDate());

            if (orderDate != null) {
                validOrderCount++;

                // Only include orders from current year
                if (orderDate.getYear() == currentYear) {
                    // Get month abbreviation (Jan, Feb, etc.)
                    String month = orderDate.getMonth().toString().substring(0, 3);

                    monthlySales.put(
                            month,
                            monthlySales.getOrDefault(month, BigDecimal.ZERO).add(order.getTotalAmount())
                    );
                }
            }
        }

        logger.info("Found {} orders with valid dates out of {} total for monthly view", validOrderCount, orders.size());

        // Convert to chart format
        List<Map<String, Object>> chartData = new ArrayList<>();

        for (Map.Entry<String, BigDecimal> entry : monthlySales.entrySet()) {
            Map<String, Object> dataPoint = new HashMap<>();
            dataPoint.put("name", entry.getKey());
            dataPoint.put("value", entry.getValue().intValue()); // Use actual value
            chartData.add(dataPoint);
        }

        return chartData;
    }

    /**
     * Get yearly sales data for the past 3 years
     */
    private List<Map<String, Object>> getYearlySalesData(List<Order> orders) {
        logger.info("Processing yearly sales data");

        // Calculate the years to include (current year and 2 previous)
        int currentYear = LocalDate.now().getYear();
        int startYear = currentYear - 2;

        // Map to store yearly sales
        Map<Integer, BigDecimal> yearlySales = new TreeMap<>();

        // Initialize years with zero
        for (int year = startYear; year <= currentYear; year++) {
            yearlySales.put(year, BigDecimal.ZERO);
        }

        // Count orders with valid dates
        int validOrderCount = 0;

        // Aggregate sales by year
        for (Order order : orders) {
            LocalDate orderDate = parseOrderDate(order.getDate());

            if (orderDate != null) {
                validOrderCount++;

                int year = orderDate.getYear();

                // Only include orders in our year range
                if (year >= startYear && year <= currentYear) {
                    yearlySales.put(
                            year,
                            yearlySales.getOrDefault(year, BigDecimal.ZERO).add(order.getTotalAmount())
                    );
                }
            }
        }

        logger.info("Found {} orders with valid dates out of {} total for yearly view", validOrderCount, orders.size());

        // Convert to chart format
        List<Map<String, Object>> chartData = new ArrayList<>();

        for (Map.Entry<Integer, BigDecimal> entry : yearlySales.entrySet()) {
            Map<String, Object> dataPoint = new HashMap<>();
            dataPoint.put("name", entry.getKey().toString());
            dataPoint.put("value", entry.getValue().intValue()); // Use actual value
            chartData.add(dataPoint);
        }

        return chartData;
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
}