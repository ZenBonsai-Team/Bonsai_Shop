package com.example.bonsai_shop;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import org.springframework.beans.factory.annotation.Autowired;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
class BonsaiShopApplicationTests {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Autowired
    private com.example.bonsai_shop.customer.service.CustomUserDetailsService customUserDetailsService;

    @Test
    void contextLoads() {
        try (Connection conn = dataSource.getConnection();
                Statement stmt = conn.createStatement()) {

            System.out.println("========== DATABASE INSPECTION ==========");
            System.out.println("Connected to: " + conn.getMetaData().getURL());

            // 1. Show all tables
            List<String> tables = new ArrayList<>();
            try (ResultSet rs = conn.getMetaData().getTables(null, null, "%", new String[] { "TABLE" })) {
                while (rs.next()) {
                    tables.add(rs.getString("TABLE_NAME"));
                }
            }
            System.out.println("Tables: " + tables);

            // Check BCrypt hash matching
            String targetHash = "$2a$10$STf1tU5lFcq6Zm4xgZRujuA7wsGd.6AK4nRH6FEL.ieVUQE8EWp3e";
            String[] candidates = {"123456", "admin", "admin123", "password", "password123", "1", "123", "1234", "12345", "falcon", "DuongNKT", "0984634913"};
            System.out.println("--- Password Hash Matching ---");
            for (String cand : candidates) {
                if (passwordEncoder.matches(cand, targetHash)) {
                    System.out.println("FOUND MATCHING PASSWORD: " + cand);
                }
            }

            // Simulate Security Authentication loading
            System.out.println("--- Simulating UserDetailsService load ---");
            String[] testEmails = {"admin@example.com", "nguyenkieutungduong@gmail.com", "duongnkthe186476@fpt.edu.vn"};
            for (String email : testEmails) {
                try {
                    org.springframework.security.core.userdetails.UserDetails userDetails = 
                            customUserDetailsService.loadUserByUsername(email);
                    System.out.printf("Loaded user: %s, enabled: %b, credentialsNonExpired: %b, authorities: %s%n",
                            userDetails.getUsername(),
                            userDetails.isEnabled(),
                            userDetails.isCredentialsNonExpired(),
                            userDetails.getAuthorities());
                    
                    boolean pwMatches = passwordEncoder.matches("123456", userDetails.getPassword());
                    System.out.printf("Password '123456' matches for %s: %b%n", email, pwMatches);
                } catch (Exception e) {
                    System.err.println("Failed to load or match for " + email + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // 2. Query flyway_schema_history if exists
            if (tables.contains("flyway_schema_history")) {
                System.out.println("--- Flyway Schema History ---");
                try (ResultSet rs = stmt.executeQuery(
                        "SELECT version, description, type, script, success FROM flyway_schema_history")) {
                    while (rs.next()) {
                        System.out.printf("Version: %s, Description: %s, Script: %s, Success: %b%n",
                                rs.getString("version"),
                                rs.getString("description"),
                                rs.getString("script"),
                                rs.getBoolean("success"));
                    }
                }
            } else {
                System.out.println("flyway_schema_history table does NOT exist!");
            }
            System.out.println("=========================================");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
