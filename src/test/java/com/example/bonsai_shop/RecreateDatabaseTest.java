package com.example.bonsai_shop;

import org.junit.jupiter.api.Test;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.Properties;

class RecreateDatabaseTest {

    @Test
    void cleanAndRecreateDb() {
        Properties props = new Properties();
        try {
            // Read application.properties to get the DB credentials dynamically
            try (InputStream input = Files.newInputStream(Paths.get("src/main/resources/application.properties"))) {
                props.load(input);
            }

            String url = props.getProperty("spring.datasource.url");
            String user = props.getProperty("spring.datasource.username", "root");
            String pass = props.getProperty("spring.datasource.password", "");

            // Extract base URL (e.g. jdbc:mysql://localhost:3306/) to connect without specifying database
            String baseUrl = "jdbc:mysql://localhost:3306/?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true";
            if (url != null && url.startsWith("jdbc:mysql://")) {
                int firstSlash = url.indexOf("/", 13); // after jdbc:mysql://
                if (firstSlash != -1) {
                    int questionMark = url.indexOf("?", firstSlash);
                    String params = (questionMark != -1) ? url.substring(questionMark) : "?useSSL=false&allowPublicKeyRetrieval=true";
                    baseUrl = url.substring(0, firstSlash + 1) + params;
                }
            }

            System.out.println("========== CLEAN & RECREATE DATABASE ==========");
            System.out.println("Connecting to MySQL server at: " + baseUrl);
            
            try (Connection conn = DriverManager.getConnection(baseUrl, user, pass);
                 Statement stmt = conn.createStatement()) {
                
                System.out.println("Connected successfully. Dropping database 'bonsai_shop' if exists...");
                stmt.executeUpdate("DROP DATABASE IF EXISTS `bonsai_shop`");
                System.out.println("Database 'bonsai_shop' dropped successfully.");
                
                System.out.println("Creating database 'bonsai_shop' with utf8mb4 encoding...");
                stmt.executeUpdate("CREATE DATABASE `bonsai_shop` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
                System.out.println("Database 'bonsai_shop' created successfully.");
                System.out.println("==============================================");
            }
        } catch (Exception e) {
            System.err.println("Error recreating database: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
