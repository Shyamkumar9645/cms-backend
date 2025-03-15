package com.cms.cms.service;

import com.cms.cms.Repository.OrganizationRepository;
import com.cms.cms.Repository.PasswordResetTokenRepository;
import com.cms.cms.Repository.UserRepository;
import com.cms.cms.model.NewOrg;
import com.cms.cms.model.PasswordResetToken;
import com.cms.cms.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
public class PasswordResetService {
    private static final Logger logger = LoggerFactory.getLogger(PasswordResetService.class);

    @Autowired
    private PasswordResetTokenRepository tokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JavaMailSender emailSender;

    @Value("${app.frontend.url:https://localhost:3000}")
    private String frontendUrl;

    @Value("${app.email.from:hello@demomailtrap.co}")
    private String emailFrom;

    /**
     * Create a password reset token for the specified email
     */
    @Transactional
    public boolean createPasswordResetTokenForEmail(String email) {
        logger.info("Creating password reset token for email: {}", email);

        // First check if the email exists in the admin users
        Optional<User> adminUser = userRepository.findByEmail(email);
        if (adminUser.isPresent()) {
            logger.info("Found admin user with email: {}", email);
            User user = adminUser.get();

            // Check if there's already an active token
            Optional<PasswordResetToken> existingToken = tokenRepository.findByEmailAndUsed(email, false);
            if (existingToken.isPresent()) {
                PasswordResetToken token = existingToken.get();
                if (!token.isExpired()) {
                    // Reuse the existing token
                    sendPasswordResetEmail(user.getEmail(), token.getToken(), "ADMIN");
                    return true;
                } else {
                    // Expire the old token
                    token.setUsed(true);
                    tokenRepository.save(token);
                }
            }

            // Create a new token
            PasswordResetToken token = PasswordResetToken.create(user.getId(), "ADMIN", user.getEmail());
            tokenRepository.save(token);

            // Send the email with the reset link
            sendPasswordResetEmail(user.getEmail(), token.getToken(), "ADMIN");
            return true;
        }

        // Check if the email exists in organization representatives
        Optional<NewOrg> organization = organizationRepository.findByRepresentativeEmail(email);
        if (organization.isPresent()) {
            logger.info("Found organization with representative email: {}", email);
            NewOrg org = organization.get();

            // Check if there's already an active token
            Optional<PasswordResetToken> existingToken = tokenRepository.findByEmailAndUsed(email, false);
            if (existingToken.isPresent()) {
                PasswordResetToken token = existingToken.get();
                if (!token.isExpired()) {
                    // Reuse the existing token
                    sendPasswordResetEmail(org.getRepresentativeEmail(), token.getToken(), "ORGANIZATION");
                    return true;
                } else {
                    // Expire the old token
                    token.setUsed(true);
                    tokenRepository.save(token);
                }
            }

            // Create a new token
            PasswordResetToken token = PasswordResetToken.create(org.getId(), "ORGANIZATION", org.getRepresentativeEmail());
            tokenRepository.save(token);

            // Send the email with the reset link
            sendPasswordResetEmail(org.getRepresentativeEmail(), token.getToken(), "ORGANIZATION");
            return true;
        }

        logger.warn("No user found with email: {}", email);
        return false; // No user found with this email
    }

    /**
     * Validate a password reset token
     */
    public boolean validatePasswordResetToken(String token) {
        logger.info("Validating password reset token");

        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
        if (!tokenOpt.isPresent()) {
            logger.warn("Token not found: {}", token);
            return false;
        }

        PasswordResetToken resetToken = tokenOpt.get();

        if (resetToken.isUsed()) {
            logger.warn("Token has already been used: {}", token);
            return false;
        }

        if (resetToken.isExpired()) {
            logger.warn("Token has expired: {}", token);
            return false;
        }

        return true;
    }

    /**
     * Reset password using token
     */
    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        logger.info("Resetting password with token");

        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
        if (!tokenOpt.isPresent()) {
            logger.warn("Token not found: {}", token);
            return false;
        }

        PasswordResetToken resetToken = tokenOpt.get();

        if (resetToken.isUsed()) {
            logger.warn("Token has already been used: {}", token);
            return false;
        }

        if (resetToken.isExpired()) {
            logger.warn("Token has expired: {}", token);
            return false;
        }

        String userType = resetToken.getUserType();
        Long userId = resetToken.getUserId();

        if ("ADMIN".equals(userType)) {
            Optional<User> userOpt = userRepository.findById(userId);
            if (!userOpt.isPresent()) {
                logger.warn("Admin user not found with id: {}", userId);
                return false;
            }

            User user = userOpt.get();
            user.setPassword(passwordEncoder.encode(newPassword));
            userRepository.save(user);

        } else if ("ORGANIZATION".equals(userType)) {
            Optional<NewOrg> orgOpt = organizationRepository.findById(userId);
            if (!orgOpt.isPresent()) {
                logger.warn("Organization not found with id: {}", userId);
                return false;
            }

            NewOrg org = orgOpt.get();
            org.setWebsitePassword(passwordEncoder.encode(newPassword));
            organizationRepository.save(org);
        } else {
            logger.warn("Unknown user type: {}", userType);
            return false;
        }

        // Mark token as used
        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        return true;
    }

    /**
     * Send password reset email
     */
    private void sendPasswordResetEmail(String email, String token, String userType) {
        try {
            String resetUrl = frontendUrl + "/reset-password?token=" + token;
            String userTypeDisplay = "ADMIN".equals(userType) ? "Administrator" : "Organization";

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(emailFrom);
            message.setTo(email);
            message.setSubject("Reset your Suraksha Pharma password");
            message.setText("Hello,\n\n" +
                    "You have requested to reset your password for your Suraksha Pharma " + userTypeDisplay + " account.\n\n" +
                    "Please click the link below to reset your password:\n" +
                    resetUrl + "\n\n" +
                    "This link will expire in 24 hours.\n\n" +
                    "If you did not request a password reset, please ignore this email.\n\n" +
                    "Regards,\nThe Suraksha Pharma Team");

            emailSender.send(message);
            logger.info("Password reset email sent to: {}", email);
        } catch (Exception e) {
            logger.error("Failed to send password reset email to {}: {}", email, e.getMessage());
        }
    }
}