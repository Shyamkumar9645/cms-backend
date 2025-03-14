package com.cms.cms.controller;

import com.cms.cms.config.JwtTokenProvider;
import com.cms.cms.model.JwtResponse;
import com.cms.cms.model.LoginRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    @Qualifier("organizationUserDetailsService")
    private UserDetailsService orgUserDetailsService;

    @Autowired
    @Qualifier("userDetailsServiceImpl")
    private UserDetailsService adminUserDetailsService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        logger.info("Received login request for username: {}", loginRequest.getUsername());

        // Try to authenticate as organization user first, then as admin
        UserDetails userDetails = findUserDetails(loginRequest.getUsername());

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("USER_NOT_FOUND", "User not found"));
        }

        // Verify password
        if (!passwordEncoder.matches(loginRequest.getPassword(), userDetails.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("INVALID_CREDENTIALS", "Invalid password"));
        }

        // Create authentication and generate token
        String userType = determineUserType(userDetails);
        Authentication authentication = createAuthentication(userDetails);
        String jwt = jwtTokenProvider.generateToken(authentication, userType);

        logger.info("{} authentication successful for username: {}", userType, loginRequest.getUsername());

        return ResponseEntity.ok(new JwtResponse(
                jwt,
                Long.valueOf(jwtTokenProvider.getExpirationInMs()),
                userDetails.getAuthorities(),
                userType
        ));
    }

    /**
     * Refresh token endpoint
     * This endpoint creates a new JWT token if the refresh token is valid
     *
     * @param request The HTTP request containing the JWT token
     * @return A new JWT token or an error response
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        logger.info("Received token refresh request");

        // Extract token from request
        String token = getJwtFromRequest(request);

        if (token == null) {
            logger.warn("No token provided for refresh");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("TOKEN_MISSING", "No token provided"));
        }

        try {
            // Check if token is valid but about to expire
            if (!jwtTokenProvider.validateToken(token)) {
                logger.warn("Invalid token provided for refresh");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("TOKEN_INVALID", "Token is invalid or expired"));
            }

            // Get username and user type from the token
            String username = jwtTokenProvider.getUsernameFromToken(token);
            String userType = jwtTokenProvider.getUserTypeFromToken(token);

            logger.info("Token valid for refresh, username: {}, userType: {}", username, userType);

            // Find user details
            UserDetails userDetails = findUserDetails(username);
            if (userDetails == null) {
                logger.warn("User not found for token refresh: {}", username);
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("USER_NOT_FOUND", "User associated with token not found"));
            }

            // Create a new authentication and generate a fresh token
            Authentication authentication = createAuthentication(userDetails);
            String newToken = jwtTokenProvider.generateToken(authentication, userType);

            logger.info("Token refresh successful for user: {}", username);

            // Optional: Blacklist the old token
            // jwtTokenProvider.blacklistToken(token);

            // Return new token
            return ResponseEntity.ok(new JwtResponse(
                    newToken,
                    Long.valueOf(jwtTokenProvider.getExpirationInMs()),
                    userDetails.getAuthorities(),
                    userType
            ));

        } catch (Exception e) {
            logger.error("Error during token refresh", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("REFRESH_ERROR", "Error refreshing token"));
        }
    }

    /**
     * Verify token endpoint
     * This endpoint checks if a token is valid and returns user information
     *
     * @param request The HTTP request containing the JWT token
     * @return User information or an error response
     */
    @GetMapping("/verify")
    public ResponseEntity<?> verifyToken(HttpServletRequest request) {
        logger.info("Received token verification request");

        // Extract token from request
        String token = getJwtFromRequest(request);

        if (token == null) {
            logger.warn("No token provided for verification");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("TOKEN_MISSING", "No token provided"));
        }

        try {
            // Validate token
            if (!jwtTokenProvider.validateToken(token)) {
                logger.warn("Invalid token provided for verification");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("TOKEN_INVALID", "Token is invalid or expired"));
            }

            // Extract information from token
            String username = jwtTokenProvider.getUsernameFromToken(token);
            String userType = jwtTokenProvider.getUserTypeFromToken(token);
            Date expirationDate = jwtTokenProvider.getExpirationDateFromToken(token);

            logger.info("Token valid, username: {}, userType: {}, expires: {}",
                    username, userType, expirationDate);

            // Create response with user information
            Map<String, Object> response = new HashMap<>();
            response.put("username", username);
            response.put("userType", userType);
            response.put("expires", expirationDate.getTime());
            response.put("isValid", true);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Error during token verification", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("VERIFICATION_ERROR", "Error verifying token"));
        }
    }

    /**
     * Endpoint for logging out users
     * This endpoint doesn't need authentication since users might be logging out with expired tokens
     *
     * @param request The HTTP request containing the JWT token
     * @return Response with logout status
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(HttpServletRequest request) {
        String jwt = getJwtFromRequest(request);
        logger.info("Received logout request");

        // Clear the security context regardless of token
        SecurityContextHolder.clearContext();

        // If token is present, add it to a blacklist
        if (jwt != null && !jwt.isEmpty()) {
            try {
                // Even if token validation fails, we still want to log the user out
                // Extract username if possible for logging purposes
                String username = null;
                try {
                    username = jwtTokenProvider.getUsernameFromToken(jwt);
                    logger.info("Logging out user: {}", username);
                } catch (Exception e) {
                    logger.warn("Could not extract username from token during logout");
                }

                // Add token to blacklist in JwtTokenProvider if implemented
                jwtTokenProvider.blacklistToken(jwt);

                logger.info("User successfully logged out");
                return ResponseEntity.ok(createSuccessResponse("Logged out successfully"));
            } catch (Exception e) {
                logger.error("Error during logout process", e);
                // Still return success since the security context was cleared
                return ResponseEntity.ok(createSuccessResponse("Logged out"));
            }
        }

        // If no token was provided, still consider it a successful logout
        return ResponseEntity.ok(createSuccessResponse("Logged out"));
    }

    /**
     * Extract JWT from request
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    /**
     * Create a success response object
     */
    private Map<String, String> createSuccessResponse(String message) {
        Map<String, String> response = new HashMap<>();
        response.put("message", message);
        response.put("status", "success");
        return response;
    }

    /**
     * Find user details by trying organization and admin services
     */
    private UserDetails findUserDetails(String username) {
        try {
            logger.debug("Trying to find organization user: {}", username);
            return orgUserDetailsService.loadUserByUsername(username);
        } catch (UsernameNotFoundException orgEx) {
            logger.debug("Not found as organization, trying admin: {}", username);
            try {
                return adminUserDetailsService.loadUserByUsername(username);
            } catch (UsernameNotFoundException adminEx) {
                logger.warn("User not found in any service: {}", username);
                return null;
            }
        }
    }

    /**
     * Determine user type based on the service that provided the user details
     */
    private String determineUserType(UserDetails userDetails) {
        // This can be improved by checking authorities or class type
        // For now, we'll use a simple check based on the same logic as before
        try {
            orgUserDetailsService.loadUserByUsername(userDetails.getUsername());
            return "ORGANIZATION";
        } catch (UsernameNotFoundException e) {
            return "ADMIN";
        }
    }

    /**
     * Create authentication token and set in security context
     */
    private Authentication createAuthentication(UserDetails userDetails) {
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
        logger.info("Created authentication with authorities: {}",
                userDetails.getAuthorities().stream()
                        .map(a -> a.getAuthority())
                        .collect(Collectors.joining(", ")));
        return authentication;
    }

    // Helper class for error responses
    public static class ErrorResponse {
        private final String error;
        private final String message;

        public ErrorResponse(String error, String message) {
            this.error = error;
            this.message = message;
        }

        public String getError() {
            return error;
        }

        public String getMessage() {
            return message;
        }
    }
}