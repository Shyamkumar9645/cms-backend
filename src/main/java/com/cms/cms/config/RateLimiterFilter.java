package com.cms.cms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Filter to apply rate limiting to API requests
 * This filter should be ordered before other security filters
 */
@Component
@Order(1) // Execute this filter before others
public class RateLimiterFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(RateLimiterFilter.class);

    @Autowired
    private RateLimiter rateLimiter;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Path prefixes to apply rate limiting to
     */
    private static final String[] RATE_LIMITED_PATHS = {
            "/api/auth/",     // Authentication endpoints
            "/api/admin/",    // Admin endpoints
            "/api/org/"       // Organization endpoints
    };

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip rate limiting if the path doesn't match our criteria
        if (!shouldRateLimit(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Get username if authenticated
        String username = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
            username = auth.getName();
        }

        // Check if request is allowed
        if (rateLimiter.allowRequest(request, path, username)) {
            filterChain.doFilter(request, response);
        } else {
            // If request exceeds limit, return 429 Too Many Requests
            response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE);
            response.setContentType("application/json");

            Map<String, Object> errorDetails = new HashMap<>();
            errorDetails.put("status", 429);
            errorDetails.put("error", "Too Many Requests");
            errorDetails.put("message", "API rate limit exceeded. Please try again later.");

            // For login endpoint, provide more specific message
            if (path.contains("/auth/login")) {
                errorDetails.put("message", "Too many login attempts. Please try again later.");
            }

            // Write error response
            objectMapper.writeValue(response.getOutputStream(), errorDetails);
        }
    }

    /**
     * Determine if the path should be rate limited
     */
    private boolean shouldRateLimit(String path) {
        if (path == null) {
            return false;
        }

        for (String prefix : RATE_LIMITED_PATHS) {
            if (path.startsWith(prefix)) {
                return true;
            }
        }

        return false;
    }
}