package com.cms.cms.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Service responsible for JWT token operations.
 * This class handles token generation, validation, and extraction of information from tokens.
 */
@Component
public class JwtTokenProvider {
    private static final Logger logger = LoggerFactory.getLogger(JwtTokenProvider.class);

    @Value("${app.jwt.secret}")
    private String jwtSecret;

    @Value("${app.jwt.expiration}")
    private int jwtExpirationInMs;

    @Value("${app.jwt.issuer:cms-project}")
    private String issuer;

    @Value("${app.jwt.refresh-expiration:604800000}")
    private int refreshTokenExpirationMs;


    /**
     * Get the signing key for JWT
     */
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Generate JWT token for a user
     *
     * @param authentication The authentication object containing user details
     * @param userType The type of user (ADMIN or ORGANIZATION)
     * @return JWT token string
     */
    public String generateToken(Authentication authentication, String userType) {
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return generateTokenForUser(userDetails, userType);
    }

    /**
     * Generate token directly from UserDetails
     *
     * @param userDetails The user details
     * @param userType The type of user
     * @return JWT token string
     */
    public String generateTokenForUser(UserDetails userDetails, String userType) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userType", userType);

        // Extract organization ID if applicable
        if ("ORGANIZATION".equals(userType) && userDetails instanceof UserDetailsWithOrg) {
            claims.put("orgId", ((UserDetailsWithOrg) userDetails).getOrgId());
        }

        // Add roles to claims
        claims.put("roles", userDetails.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .toList());

        return createToken(claims, userDetails.getUsername());
    }

    /**
     * Generate token with default user type (ADMIN)
     * Method overload for backward compatibility
     */
    public String generateToken(Authentication authentication) {
        return generateToken(authentication, "ADMIN");
    }

    /**
     * Generate a refresh token
     *
     * @param username The username
     * @return Refresh token string
     */
    public String generateRefreshToken(String username) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + refreshTokenExpirationMs);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setId(UUID.randomUUID().toString())
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Create a token with the specified claims and subject
     */
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationInMs);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .setIssuer(issuer)
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * Extract username from token
     */
    public String getUsernameFromToken(String token) {
        Claims claims = extractAllClaims(token);
        return claims.getSubject();
    }

    /**
     * Extract user type from token
     */
    public String getUserTypeFromToken(String token) {
        Claims claims = extractAllClaims(token);
        // Default to "ADMIN" if userType claim is not present
        return claims.get("userType", String.class) != null ?
                claims.get("userType", String.class) : "ADMIN";
    }

    /**
     * Extract all claims from a token
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Validate a token
     */
    public boolean validateToken(String authToken) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(authToken);
            return true;
        } catch (MalformedJwtException e) {
            logger.error("Invalid JWT token: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("JWT token is expired: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("JWT token is unsupported: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims string is empty: {}", e.getMessage());
        } catch (Exception e) {
            logger.error("JWT validation error: {}", e.getMessage());
        }
        return false;
    }

    /**
     * Get token expiration time in milliseconds
     */
    public int getExpirationInMs() {
        return jwtExpirationInMs;
    }

    /**
     * Get the expiration date from token
     */
    public Date getExpirationDateFromToken(String token) {
        Claims claims = extractAllClaims(token);
        return claims.getExpiration();
    }

    /**
     * Interface for user details with organization ID
     * This decouples the class from specific implementation
     */
    public interface UserDetailsWithOrg {
        Long getOrgId();
    }
}