package com.cms.cms.controller;

import com.cms.cms.config.JwtTokenProvider;
import com.cms.cms.model.*;
import com.cms.cms.service.AuthService;
import com.cms.cms.service.UserDetailsImpl;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private AuthService authService;

    @Autowired
    @Qualifier("userDetailsServiceImpl")
    private UserDetailsService adminUserDetailsService;

    @Autowired
    @Qualifier("organizationUserDetailsService")
    private UserDetailsService orgUserDetailsService;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        logger.info("Received login request for username: {}", loginRequest.getUsername());

        try {
            // First try to authenticate as admin
            try {
                Authentication adminAuth = authenticationManager.authenticate(
                        new UsernamePasswordAuthenticationToken(
                                loginRequest.getUsername(),
                                loginRequest.getPassword()
                        )
                );

                SecurityContextHolder.getContext().setAuthentication(adminAuth);
                String jwt = jwtTokenProvider.generateToken(adminAuth, "ADMIN");

                logger.info("Admin authentication successful for username: {}", loginRequest.getUsername());

                return ResponseEntity.ok(new JwtResponse(
                        jwt,
                        Long.valueOf(jwtTokenProvider.getExpirationInMs()),
                        ((UserDetails) adminAuth.getPrincipal()).getAuthorities(),
                        "ADMIN"
                ));
            } catch (BadCredentialsException e) {
                // Admin authentication failed, try organization authentication
                logger.info("Admin authentication failed, trying organization credentials for: {}",
                        loginRequest.getUsername());

                try {
                    // Try to load org user details
                    UserDetails orgUserDetails = orgUserDetailsService.loadUserByUsername(loginRequest.getUsername());

                    // Manually verify password since we're not using the authentication manager directly
                    Authentication orgAuth = authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    loginRequest.getUsername(),
                                    loginRequest.getPassword()
                            )
                    );

                    SecurityContextHolder.getContext().setAuthentication(orgAuth);
                    String jwt = jwtTokenProvider.generateToken(orgAuth, "ORGANIZATION");

                    logger.info("Organization authentication successful for username: {}", loginRequest.getUsername());

                    return ResponseEntity.ok(new JwtResponse(
                            jwt,
                            Long.valueOf(jwtTokenProvider.getExpirationInMs()),
                            orgUserDetails.getAuthorities(),
                            "ORGANIZATION"
                    ));
                } catch (Exception orgEx) {
                    // Both authentication methods failed
                    logger.error("Both authentication methods failed for username: {}", loginRequest.getUsername());
                    throw new BadCredentialsException("Invalid username or password");
                }
            }
        } catch (BadCredentialsException ex) {
            // Specific exception for bad credentials
            logger.error("Authentication failed for username: {}. Reason: Bad credentials", loginRequest.getUsername());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("INVALID_CREDENTIALS", "Invalid username or password"));
        } catch (AuthenticationException ex) {
            // General authentication exception catch-all
            logger.error("Authentication failed for username: {}. Error: {}", loginRequest.getUsername(), ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new ErrorResponse("AUTH_FAILED", "Authentication failed"));
        }
    }

    // Other methods remain the same...

    // Helper classes
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