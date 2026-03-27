package com.example.auth;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordGenerator {
    public static void main(String[] args) {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

        String adminPassword = encoder.encode("admin123");
        String testPassword = encoder.encode("user123");

        System.out.println("Admin password (admin123): " + adminPassword);
        System.out.println("Test password (user123): " + testPassword);
    }
}
