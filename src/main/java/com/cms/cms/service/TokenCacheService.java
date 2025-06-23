package com.cms.cms.service;

import com.cms.cms.config.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class TokenCacheService {
    private static final Logger logger = LoggerFactory.getLogger(TokenCacheService.class);

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Value("${app.jwt.expiration}")
    private int jwtExpirationInMs;

    private static final String TOKEN_PREFIX = "token:";
    private static final String USER_TOKEN_PREFIX = "user:token:";
    private static final String BLACKLIST_PREFIX = "blacklist:";

    /**
     * Store a token in Redis cache
     *
     * @param token The JWT token
     * @param username The username associated with the token
     * @param userType The type of user (ADMIN or ORGANIZATION)
     */
    public void cacheToken(String token, String username, String userType) {
        try {
            // Store token with details, useful for validation and lookup
            String tokenKey = TOKEN_PREFIX + token;
            TokenInfo tokenInfo = new TokenInfo(username, userType);
            redisTemplate.opsForValue().set(tokenKey, tokenInfo, jwtExpirationInMs, TimeUnit.MILLISECONDS);

            // Store reference by username for finding user's tokens
            String userKey = USER_TOKEN_PREFIX + username;
            redisTemplate.opsForSet().add(userKey, token);
            redisTemplate.expire(userKey, jwtExpirationInMs, TimeUnit.MILLISECONDS);

            logger.info("Token cached for user: {}", username);
        } catch (Exception e) {
            // Graceful degradation - if Redis fails, system can still work with JWT validation
            logger.error("Failed to cache token: {}", e.getMessage());
        }
    }

    /**
     * Check if a token is valid in the cache
     *
     * @param token The JWT token
     * @return True if token is valid and cached
     */
    public boolean isTokenCached(String token) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(TOKEN_PREFIX + token));
        } catch (Exception e) {
            logger.error("Error checking token in cache: {}", e.getMessage());
            // Default to standard JWT validation if Redis fails
            return false;
        }
    }

    /**
     * Get username from cached token
     *
     * @param token The JWT token
     * @return Username or null if not found
     */
    public String getUsernameFromCache(String token) {
        try {
            TokenInfo info = (TokenInfo) redisTemplate.opsForValue().get(TOKEN_PREFIX + token);
            return info != null ? info.getUsername() : null;
        } catch (Exception e) {
            logger.error("Error retrieving username from cache: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get user type from cached token
     *
     * @param token The JWT token
     * @return User type or null if not found
     */
    public String getUserTypeFromCache(String token) {
        try {
            TokenInfo info = (TokenInfo) redisTemplate.opsForValue().get(TOKEN_PREFIX + token);
            return info != null ? info.getUserType() : null;
        } catch (Exception e) {
            logger.error("Error retrieving user type from cache: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Invalidate a token (add to blacklist)
     *
     * @param token The JWT token
     */
    public void blacklistToken(String token) {
        try {
            // Get existing token info if available
            TokenInfo info = (TokenInfo) redisTemplate.opsForValue().get(TOKEN_PREFIX + token);

            if (info != null) {
                // Remove from active tokens
                redisTemplate.delete(TOKEN_PREFIX + token);

                // Remove from user's token set
                redisTemplate.opsForSet().remove(USER_TOKEN_PREFIX + info.getUsername(), token);
            }

            // Add to blacklist for remaining validity period
            long remainingTtl = jwtTokenProvider.validateTokenAndGetRemainingTime(token);
            if (remainingTtl > 0) {
                redisTemplate.opsForValue().set(BLACKLIST_PREFIX + token, true, remainingTtl, TimeUnit.MILLISECONDS);
                logger.info("Token blacklisted for {} ms", remainingTtl);
            }
        } catch (Exception e) {
            logger.error("Failed to blacklist token: {}", e.getMessage());
        }
    }

    /**
     * Check if a token is blacklisted
     *
     * @param token The JWT token
     * @return True if token is blacklisted
     */
    public boolean isTokenBlacklisted(String token) {
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(BLACKLIST_PREFIX + token));
        } catch (Exception e) {
            logger.error("Error checking blacklist: {}", e.getMessage());
            // Safer to assume token might be blacklisted if Redis fails
            return true;
        }
    }

    /**
     * Generate and cache a new token
     *
     * @param authentication The authentication object
     * @param userType The user type
     * @return The generated JWT token
     */
    public String generateAndCacheToken(Authentication authentication, String userType) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        String username = userDetails.getUsername();

        // Generate token using existing provider
        String token = jwtTokenProvider.generateToken(authentication, userType);

        // Cache the token
        cacheToken(token, username, userType);

        return token;
    }

    /**
     * Inner class to store token information
     */
    public static class TokenInfo {
        private String username;
        private String userType;

        public TokenInfo() {
            // Default constructor for deserialization
        }

        public TokenInfo(String username, String userType) {
            this.username = username;
            this.userType = userType;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getUserType() {
            return userType;
        }

        public void setUserType(String userType) {
            this.userType = userType;
        }
    }
}