package com.cms.cms.controller;
import com.cms.cms.model.*;
import com.cms.cms.config.JwtTokenProvider;
import com.cms.cms.service.AuthService;
import com.cms.cms.service.UserDetailsImpl;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.bind.annotation.*;

import java.util.Date;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthService authService;

    @Value("${app.jwt.refresh-cookie-name:refresh_token}")
    private String refreshCookieName;

    @Value("${app.jwt.cookie-secure:false}")
    private boolean secureCookie;
    @Autowired
    private UserDetailsService userDetailsService; // This is the instance

    public AuthController(AuthenticationManager authenticationManager,
                          JwtTokenProvider jwtTokenProvider,
                          AuthService authService) {
        this.authenticationManager = authenticationManager;
        this.jwtTokenProvider = jwtTokenProvider;
        this.authService = authService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest,
                                              HttpServletResponse response) {
        logger.info("Processing login request for username: {}", loginRequest.getUsername());

        try {
            // Attempt authentication
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
            );
            logger.info("Authentication successful for username: {}", loginRequest.getUsername());

            SecurityContextHolder.getContext().setAuthentication(authentication);

            // Generate tokens
            String jwt = jwtTokenProvider.generateToken(authentication);
            String refreshToken = jwtTokenProvider.generateRefreshToken(loginRequest.getUsername());

            // Set refresh token as HTTP-only cookie
            Cookie refreshCookie = new Cookie(refreshCookieName, refreshToken);
            refreshCookie.setHttpOnly(true);
            refreshCookie.setSecure(secureCookie);
            refreshCookie.setPath("/api/auth/refresh");
            refreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
            response.addCookie(refreshCookie);

            logger.info("JWT generated successfully for username: {}", loginRequest.getUsername());

            // In AuthController.java, line 83 and 174
            return ResponseEntity.ok(new JwtResponse(
                    jwt,
                    Long.valueOf(jwtTokenProvider.getExpirationInMs()), // Convert int to Long
                    ((UserDetailsImpl) authentication.getPrincipal()).getAuthorities()
            ));
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

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signupRequest) {
        logger.info("Processing registration request for username: {}", signupRequest.getUsername());

        if (authService.existsByUsername(signupRequest.getUsername())) {
            logger.warn("Registration failed - username already taken: {}", signupRequest.getUsername());
            return ResponseEntity.badRequest().body(new ErrorResponse("USERNAME_TAKEN", "Username is already taken"));
        }

        if (authService.existsByEmail(signupRequest.getEmail())) {
            logger.warn("Registration failed - email already in use: {}", signupRequest.getEmail());
            return ResponseEntity.badRequest().body(new ErrorResponse("EMAIL_TAKEN", "Email is already in use"));
        }

        try {
            User user = authService.createUser(signupRequest);
            logger.info("User registered successfully: {}", signupRequest.getUsername());
            return ResponseEntity.ok(new SuccessResponse("User registered successfully"));
        } catch (Exception e) {
            logger.error("Error during user registration: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("REGISTRATION_FAILED", "Registration failed: " + e.getMessage()));
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) {
        // Extract refresh token from cookie
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("REFRESH_TOKEN_MISSING", "Refresh token is missing"));
        }

        String refreshToken = null;
        for (Cookie cookie : cookies) {
            if (refreshCookieName.equals(cookie.getName())) {
                refreshToken = cookie.getValue();
                break;
            }
        }

        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("REFRESH_TOKEN_MISSING", "Refresh token is missing"));
        }

        // Validate refresh token
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("INVALID_REFRESH_TOKEN", "Invalid refresh token"));
        }

        // Extract username from refresh token
        String username = jwtTokenProvider.getUsernameFromToken(refreshToken);

        // Generate new access token

        UserDetailsImpl userDetails = (UserDetailsImpl) userDetailsService.loadUserByUsername(username);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());

        String newAccessToken = jwtTokenProvider.generateToken(authentication);

        // Create a new refresh token
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(username);

        // Update refresh token cookie
        Cookie newRefreshCookie = new Cookie(refreshCookieName, newRefreshToken);
        newRefreshCookie.setHttpOnly(true);
        newRefreshCookie.setSecure(secureCookie);
        newRefreshCookie.setPath("/api/auth/refresh");
        newRefreshCookie.setMaxAge(7 * 24 * 60 * 60); // 7 days
        response.addCookie(newRefreshCookie);

        return ResponseEntity.ok(new JwtResponse(
                newAccessToken,
                Long.valueOf(jwtTokenProvider.getExpirationInMs()),
                userDetails.getAuthorities()
        ));
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout(HttpServletResponse response) {
        // Clear the refresh token cookie
        Cookie cookie = new Cookie(refreshCookieName, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(secureCookie);
        cookie.setPath("/api/auth/refresh");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return ResponseEntity.ok(new SuccessResponse("Logout successful"));
    }

    @GetMapping("/verify")
    public ResponseEntity<?> verifyToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("INVALID_TOKEN", "Invalid token format"));
        }

        String token = authHeader.substring(7);

        if (jwtTokenProvider.validateToken(token)) {
            String username = jwtTokenProvider.getUsernameFromToken(token);
            Date expiryDate = jwtTokenProvider.getExpirationDateFromToken(token);

            TokenInfo tokenInfo = new TokenInfo();
            tokenInfo.setUsername(username);
            tokenInfo.setExpires(expiryDate.getTime());
            tokenInfo.setValid(true);

            return ResponseEntity.ok(tokenInfo);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponse("INVALID_TOKEN", "Token is invalid or expired"));
        }
    }

    // Helper response classes
    @lombok.Data
    public static class ErrorResponse {
        private final String error;
        private final String message;
    }

    @lombok.Data
    public static class SuccessResponse {
        private final String message;
    }

    @lombok.Data
    public static class TokenInfo {
        private String username;
        private long expires;
        private boolean valid;
    }
}