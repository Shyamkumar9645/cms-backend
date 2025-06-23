package com.cms.cms.config;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Redis-based API rate limiter for protecting endpoints from abuse
 */
@Component
public class RateLimiter {
    private static final Logger logger = LoggerFactory.getLogger(RateLimiter.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // Rate limiting configuration
    private static final String RATE_LIMITER_PREFIX = "rate:";
    private static final String IP_LIMITER_PREFIX = "rate:ip:";
    private static final String USER_LIMITER_PREFIX = "rate:user:";
    private static final String ENDPOINT_LIMITER_PREFIX = "rate:endpoint:";

    // Default limits
    private static final int DEFAULT_LIMIT = 100; // requests
    private static final int DEFAULT_TIMEFRAME = 60; // seconds
    private static final int LOGIN_ENDPOINT_LIMIT = 50; // 5 login attempts
    private static final int LOGIN_TIMEFRAME = 300; // 5 minutes

    /**
     * Check if request exceeds rate limit
     *
     * @param request The HTTP request
     * @param endpoint The endpoint being accessed
     * @param username The username (if authenticated)
     * @return True if request should be allowed, false otherwise
     */
    public boolean allowRequest(HttpServletRequest request, String endpoint, String username) {
        try {
            String clientIp = getClientIp(request);
            boolean isLoginEndpoint = endpoint.contains("/auth/login");

            // Different rate limits based on endpoint
            int limit = isLoginEndpoint ? LOGIN_ENDPOINT_LIMIT : DEFAULT_LIMIT;
            int timeframe = isLoginEndpoint ? LOGIN_TIMEFRAME : DEFAULT_TIMEFRAME;

            // Check IP-based rate limiting
            String ipKey = IP_LIMITER_PREFIX + clientIp;
            if (!checkAndIncrement(ipKey, limit, timeframe)) {
                logger.warn("Rate limit exceeded for IP: {}", clientIp);
                return false;
            }

            // Check endpoint-based rate limiting
            String endpointKey = ENDPOINT_LIMITER_PREFIX + endpoint.replaceAll("/", "_");
            if (!checkAndIncrement(endpointKey, limit * 5, timeframe)) { // Higher limit for endpoints
                logger.warn("Rate limit exceeded for endpoint: {}", endpoint);
                return false;
            }

            // For authenticated users, also check username-based limiting
            if (username != null && !username.isEmpty()) {
                String userKey = USER_LIMITER_PREFIX + username;
                if (!checkAndIncrement(userKey, limit, timeframe)) {
                    logger.warn("Rate limit exceeded for user: {}", username);
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            logger.error("Error in rate limiter, allowing request", e);
            return true; // In case of error, allow request to proceed
        }
    }

    /**
     * Check if key exists, increment its value, and create it if it doesn't exist
     *
     * @param key The rate limiting key
     * @param limit The maximum number of requests allowed
     * @param timeframeSeconds The timeframe in seconds
     * @return True if request is within limit, false otherwise
     */
    private boolean checkAndIncrement(String key, int limit, int timeframeSeconds) {
        Long current = redisTemplate.opsForValue().increment(key, 1);

        // If this is the first request, set expiry
        if (current != null && current == 1) {
            redisTemplate.expire(key, timeframeSeconds, TimeUnit.SECONDS);
        }

        // Allow if within limit
        return current != null && current <= limit;
    }

    /**
     * Get client IP address from request
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // Get the first IP in case of multiple proxies
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Reset rate limit for a user (useful after successful password changes, etc.)
     */
    public void resetUserRateLimit(String username) {
        try {
            if (username != null && !username.isEmpty()) {
                String userKey = USER_LIMITER_PREFIX + username;
                redisTemplate.delete(userKey);
                logger.info("Rate limit reset for user: {}", username);
            }
        } catch (Exception e) {
            logger.error("Error resetting rate limit for user: {}", username, e);
        }
    }
}