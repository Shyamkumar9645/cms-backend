package com.cms.cms.controller;

import com.cms.cms.service.PasswordResetService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class PasswordResetController {
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetController.class);

    @Autowired
    private PasswordResetService passwordResetService;

    /**
     * Request a password reset - generates a token and sends an email
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<?> requestPasswordReset(@RequestBody Map<String, String> request) {
        String email = request.get("email");

        if (email == null || email.trim().isEmpty()) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Email is required");
            return ResponseEntity.badRequest().body(response);
        }

        logger.info("Password reset request received for email: {}", email);

        passwordResetService.createPasswordResetTokenForEmail(email);

        // Always return success to prevent email enumeration attacks
        Map<String, String> response = new HashMap<>();
        response.put("message", "If the email exists in our system, a password reset link has been sent.");
        response.put("status", "success");

        return ResponseEntity.ok(response);
    }

    /**
     * Validate a password reset token
     */
    @PostMapping("/validate-reset-token")
    public ResponseEntity<?> validateResetToken(@RequestBody Map<String, String> request) {
        String token = request.get("token");

        if (token == null || token.trim().isEmpty()) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Token is required");
            return ResponseEntity.badRequest().body(response);
        }

        logger.info("Token validation request received");

        boolean isValid = passwordResetService.validatePasswordResetToken(token);

        if (isValid) {
            Map<String, Object> response = new HashMap<>();
            response.put("valid", true);
            response.put("message", "Token is valid");
            return ResponseEntity.ok(response);
        } else {
            Map<String, Object> response = new HashMap<>();
            response.put("valid", false);
            response.put("message", "Token is invalid or expired");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    /**
     * Reset password with a token
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String, String> request) {
        String token = request.get("token");
        String newPassword = request.get("newPassword");

        if (token == null || token.trim().isEmpty()) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Token is required");
            return ResponseEntity.badRequest().body(response);
        }

        if (newPassword == null || newPassword.trim().isEmpty()) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "New password is required");
            return ResponseEntity.badRequest().body(response);
        }

        if (newPassword.length() < 8) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Password must be at least 8 characters long");
            return ResponseEntity.badRequest().body(response);
        }

        logger.info("Password reset request received");

        boolean result = passwordResetService.resetPassword(token, newPassword);

        if (result) {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Password has been reset successfully");
            response.put("status", "success");
            return ResponseEntity.ok(response);
        } else {
            Map<String, String> response = new HashMap<>();
            response.put("message", "Failed to reset password. Token may be invalid or expired.");
            response.put("status", "error");
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }
}