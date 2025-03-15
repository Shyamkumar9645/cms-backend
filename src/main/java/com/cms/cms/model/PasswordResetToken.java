package com.cms.cms.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
import lombok.Data;

@Entity
@Table(name = "password_reset_tokens")
@Data
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(name = "user_type", nullable = false)
    private String userType; // ADMIN or ORGANIZATION

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "used", nullable = false)
    private boolean used = false;

    // Create a new token with default 24-hour expiration
    public static PasswordResetToken create(Long userId, String userType, String email) {
        PasswordResetToken token = new PasswordResetToken();
        token.setToken(UUID.randomUUID().toString());
        token.setUserId(userId);
        token.setUserType(userType);
        token.setEmail(email);
        token.setExpiryDate(Instant.now().plusSeconds(86400)); // 24 hours
        token.setUsed(false);
        return token;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(this.expiryDate);
    }
}