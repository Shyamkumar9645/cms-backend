package com.cms.cms.controller;

import com.cms.cms.Repository.OrganizationRepository;
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
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private OrganizationRepository organizationRepository;

    @Autowired
    @Qualifier("userDetailsServiceImpl")
    private UserDetailsService adminUserDetailsService;

    @Autowired
    @Qualifier("organizationUserDetailsService")
    private UserDetailsService orgUserDetailsService;


    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {
        logger.info("Received login request for username: {}", loginRequest.getUsername());

        // First check if this is an organization user
        boolean isOrgUser = false;
        UserDetails userDetails = null;

        try {
            logger.info("Trying to find organization user: {}", loginRequest.getUsername());
            userDetails = orgUserDetailsService.loadUserByUsername(loginRequest.getUsername());
            isOrgUser = true;
            logger.info("Found organization user: {}", userDetails.getUsername());
        } catch (UsernameNotFoundException orgEx) {
            logger.warn("Not found as organization: {}", orgEx.getMessage());
            // Not found as org, try as admin user
            try {
                userDetails = adminUserDetailsService.loadUserByUsername(loginRequest.getUsername());
            } catch (UsernameNotFoundException adminEx) {
                // User doesn't exist in either repository
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("USER_NOT_FOUND", "User not found"));
            }
        }

        // Now we have valid userDetails, verify the password manually
        if (!passwordEncoder.matches(loginRequest.getPassword(), userDetails.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("INVALID_CREDENTIALS", "Invalid password"));
        }

        // Password is valid, create authentication token
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        // Set authentication in context
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Generate JWT token with proper user type
        String userType = isOrgUser ? "ORGANIZATION" : "ADMIN";
        String jwt = jwtTokenProvider.generateToken(authentication, userType);

        logger.info("{} authentication successful for username: {}", userType, loginRequest.getUsername());

        return ResponseEntity.ok(new JwtResponse(
                jwt,
                Long.valueOf(jwtTokenProvider.getExpirationInMs()),
                userDetails.getAuthorities(),
                userType
        ));
    }
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