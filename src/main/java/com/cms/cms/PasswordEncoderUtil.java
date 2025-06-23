package com.cms.cms;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;


import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

public class PasswordEncoderUtil {

    public static void main(String[] args) {
        // Create the same password encoder as in your SecurityConfig
        PasswordEncoder encoder = new BCryptPasswordEncoder();

        // Passwords to encode
        String[] passwords = {
                "admin"
        };

        System.out.println("Encoded Passwords:");
        System.out.println("=================");

        for (String password : passwords) {
            String encoded = encoder.encode(password);
            System.out.println("Raw password: " + password);
            System.out.println("Encoded: " + encoded);
            System.out.println("SQL: INSERT INTO users (username, password, email) VALUES ('admin1', '" + encoded + "', 'email@example.com');");
            System.out.println();
        }
    }
}