package com.cms.cms.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Service to handle caching of dashboard data in Redis
 */
@Service
public class DashboardCacheService {
    private static final Logger logger = LoggerFactory.getLogger(DashboardCacheService.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // Cache keys
    private static final String DASHBOARD_SUMMARY_KEY = "dashboard:summary";
    private static final String RECENT_ORDERS_KEY = "dashboard:recent-orders";
    private static final String SALES_DATA_KEY_PREFIX = "dashboard:sales-data:";

    // TTL (Time to Live) values
    private static final long DASHBOARD_SUMMARY_TTL = 10; // 10 minutes
    private static final long RECENT_ORDERS_TTL = 5; // 5 minutes
    private static final long SALES_DATA_TTL = 15; // 15 minutes

    /**
     * Get dashboard summary from cache
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getDashboardSummary() {
        try {
            Object cachedData = redisTemplate.opsForValue().get(DASHBOARD_SUMMARY_KEY);
            if (cachedData != null) {
                logger.debug("Dashboard summary cache hit");
                return (Map<String, Object>) cachedData;
            }
        } catch (Exception e) {
            logger.error("Error retrieving dashboard summary from cache", e);
        }
        logger.debug("Dashboard summary cache miss");
        return null;
    }

    /**
     * Cache dashboard summary
     */
    public void cacheDashboardSummary(Map<String, Object> summaryData) {
        try {
            redisTemplate.opsForValue().set(
                    DASHBOARD_SUMMARY_KEY,
                    summaryData,
                    DASHBOARD_SUMMARY_TTL,
                    TimeUnit.MINUTES
            );
            logger.debug("Dashboard summary cached successfully");
        } catch (Exception e) {
            logger.error("Error caching dashboard summary", e);
        }
    }

    /**
     * Get recent orders from cache
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getRecentOrders() {
        try {
            Object cachedData = redisTemplate.opsForValue().get(RECENT_ORDERS_KEY);
            if (cachedData != null) {
                logger.debug("Recent orders cache hit");
                return (List<Map<String, Object>>) cachedData;
            }
        } catch (Exception e) {
            logger.error("Error retrieving recent orders from cache", e);
        }
        logger.debug("Recent orders cache miss");
        return null;
    }

    /**
     * Cache recent orders
     */
    public void cacheRecentOrders(List<Map<String, Object>> ordersData) {
        try {
            redisTemplate.opsForValue().set(
                    RECENT_ORDERS_KEY,
                    ordersData,
                    RECENT_ORDERS_TTL,
                    TimeUnit.MINUTES
            );
            logger.debug("Recent orders cached successfully");
        } catch (Exception e) {
            logger.error("Error caching recent orders", e);
        }
    }

    /**
     * Get sales data from cache for specific period
     */
    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> getSalesData(String period) {
        try {
            String key = SALES_DATA_KEY_PREFIX + period;
            Object cachedData = redisTemplate.opsForValue().get(key);
            if (cachedData != null) {
                logger.debug("Sales data cache hit for period: {}", period);
                return (List<Map<String, Object>>) cachedData;
            }
        } catch (Exception e) {
            logger.error("Error retrieving sales data from cache for period: {}", period, e);
        }
        logger.debug("Sales data cache miss for period: {}", period);
        return null;
    }

    /**
     * Cache sales data for specific period
     */
    public void cacheSalesData(String period, List<Map<String, Object>> salesData) {
        try {
            String key = SALES_DATA_KEY_PREFIX + period;
            redisTemplate.opsForValue().set(
                    key,
                    salesData,
                    SALES_DATA_TTL,
                    TimeUnit.MINUTES
            );
            logger.debug("Sales data cached successfully for period: {}", period);
        } catch (Exception e) {
            logger.error("Error caching sales data for period: {}", period, e);
        }
    }

    /**
     * Clear dashboard summary cache
     */
    public void clearDashboardSummaryCache() {
        try {
            redisTemplate.delete(DASHBOARD_SUMMARY_KEY);
            logger.info("Dashboard summary cache cleared");
        } catch (Exception e) {
            logger.error("Error clearing dashboard summary cache", e);
        }
    }

    /**
     * Clear recent orders cache
     */
    public void clearRecentOrdersCache() {
        try {
            redisTemplate.delete(RECENT_ORDERS_KEY);
            logger.info("Recent orders cache cleared");
        } catch (Exception e) {
            logger.error("Error clearing recent orders cache", e);
        }
    }

    /**
     * Clear sales data cache for specific period
     */
    public void clearSalesDataCache(String period) {
        try {
            String key = SALES_DATA_KEY_PREFIX + period;
            redisTemplate.delete(key);
            logger.info("Sales data cache cleared for period: {}", period);
        } catch (Exception e) {
            logger.error("Error clearing sales data cache for period: {}", period, e);
        }
    }

    /**
     * Clear all dashboard caches
     */
    public void clearAllDashboardCaches() {
        try {
            // Clear dashboard summary
            clearDashboardSummaryCache();

            // Clear recent orders
            clearRecentOrdersCache();

            // Clear sales data for all periods
            String[] periods = {"daily", "weekly", "monthly", "yearly"};
            for (String period : periods) {
                clearSalesDataCache(period);
            }

            logger.info("All dashboard caches cleared successfully");
        } catch (Exception e) {
            logger.error("Error clearing all dashboard caches", e);
        }
    }
}