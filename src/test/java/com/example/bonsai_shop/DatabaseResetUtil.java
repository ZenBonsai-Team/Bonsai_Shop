package com.example.bonsai_shop;

import java.sql.DriverManager;

/**
 * Tiện ích cục bộ dùng để reset CSDL bonsai_shop.
 * Chạy trực tiếp dưới dạng Java Application (không phải unit test).
 */
public class DatabaseResetUtil {

    public static void main(String[] args) throws Exception {
        dropAndCreateBonsaiShopDatabase();
    }

    public static void dropAndCreateBonsaiShopDatabase() throws Exception {
        if (!"YES".equals(System.getenv("RESET_BONSAI_DB"))) {
            throw new IllegalStateException("Set RESET_BONSAI_DB=YES before running this destructive reset.");
        }

        var mysqlUrl = System.getenv().getOrDefault(
                "RESET_BONSAI_MYSQL_URL",
                "jdbc:mysql://localhost:3306/?useSSL=false&serverTimezone=Asia/Ho_Chi_Minh&allowPublicKeyRetrieval=true&characterEncoding=UTF-8");
        var username = System.getenv().getOrDefault("RESET_BONSAI_MYSQL_USER", "root");
        var password = System.getenv().getOrDefault("RESET_BONSAI_MYSQL_PASSWORD", "123456");
        var databaseName = System.getenv().getOrDefault("RESET_BONSAI_DATABASE", "bonsai_shop");

        try (var connection = DriverManager.getConnection(mysqlUrl, username, password);
                var statement = connection.createStatement()) {
            statement.executeUpdate("DROP DATABASE IF EXISTS `" + databaseName + "`");
            statement.executeUpdate(
                    "CREATE DATABASE `" + databaseName + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }
    }
}
