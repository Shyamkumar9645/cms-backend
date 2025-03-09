package com.cms.cms.controller;

import com.cms.cms.config.JwtTokenProvider;
import com.cms.cms.model.JwtResponse;
import com.cms.cms.model.LoginRequest;
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