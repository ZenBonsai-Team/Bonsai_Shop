package com.example.bonsai_shop;

import org.junit.jupiter.api.Test;
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
