package com.example.bonsai_shop;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FinancialLedgerMigrationVerificationTest {

    private static final String VERIFY_SCHEMA = "bonsai_shop_migration_verify";

    @Test
    void cleanFlywayMigrationCreatesFinancialLedgerSchema() throws Exception {
        Properties properties = loadDatasourceProperties();
        String appUrl = properties.getProperty("spring.datasource.url");
        String username = properties.getProperty("spring.datasource.username");
        String password = properties.getProperty("spring.datasource.password", "");
        String serverUrl = toServerUrl(appUrl);
        String verifyUrl = toSchemaUrl(appUrl, VERIFY_SCHEMA);

        try (Connection serverConnection = DriverManager.getConnection(serverUrl, username, password);
             Statement statement = serverConnection.createStatement()) {
            statement.executeUpdate("DROP DATABASE IF EXISTS `" + VERIFY_SCHEMA + "`");
            statement.executeUpdate("CREATE DATABASE `" + VERIFY_SCHEMA + "` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci");
        }

        try {
            try {
                Flyway.configure()
                        .dataSource(verifyUrl, username, password)
                        .locations("classpath:db/migration")
                        .cleanDisabled(false)
                        .load()
                        .migrate();
            } catch (Exception migrationFailure) {
                throw new AssertionError(latestForeignKeyError(verifyUrl, username, password), migrationFailure);
            }

            try (Connection connection = DriverManager.getConnection(verifyUrl, username, password)) {
                assertThat(tableExists(connection, VERIFY_SCHEMA, "financial_ledger")).isTrue();
                assertThat(tableExists(connection, VERIFY_SCHEMA, "financial_adjustment_log")).isFalse();

                assertThat(columns(connection, VERIFY_SCHEMA, "financial_ledger")).contains(
                        "FinancialLedgerID",
                        "OrderID",
                        "RelatedPaymentID",
                        "RecordedByID",
                        "LedgerType",
                        "Amount",
                        "Direction",
                        "FaultParty",
                        "Reason",
                        "EvidenceNote",
                        "ExternalReference",
                        "LedgerStatus",
                        "RecognizedAt",
                        "CreatedAt",
                        "CompletedRevenueActiveKey"
                );

                assertThat(columnType(connection, VERIFY_SCHEMA, "financial_ledger", "Amount"))
                        .startsWith("decimal(15,2)");
                assertThat(isGeneratedColumn(connection, VERIFY_SCHEMA, "financial_ledger", "CompletedRevenueActiveKey"))
                        .isTrue();

                assertThat(indexes(connection, VERIFY_SCHEMA, "financial_ledger")).contains(
                        "uk_fl_completed_revenue_active",
                        "idx_fl_order",
                        "idx_fl_payment",
                        "idx_fl_recorded_by",
                        "idx_fl_type",
                        "idx_fl_status",
                        "idx_fl_recognized_at",
                        "idx_fl_fault_party"
                );

                assertThat(foreignKeys(connection, VERIFY_SCHEMA, "financial_ledger")).contains(
                        "fk_fl_order",
                        "fk_fl_payment",
                        "fk_fl_user"
                );
            }
        } finally {
            try (Connection serverConnection = DriverManager.getConnection(serverUrl, username, password);
                 Statement statement = serverConnection.createStatement()) {
                statement.executeUpdate("DROP DATABASE IF EXISTS `" + VERIFY_SCHEMA + "`");
            }
        }
    }

    private Properties loadDatasourceProperties() throws Exception {
        Properties properties = new Properties();
        Path local = Path.of("src/main/resources/application-local.properties");
        Path fallback = Path.of("src/main/resources/application.properties");
        try (InputStream inputStream = Files.newInputStream(Files.exists(local) ? local : fallback)) {
            properties.load(inputStream);
        }
        return properties;
    }

    private String toServerUrl(String appUrl) {
        int schemaStart = appUrl.indexOf("/", "jdbc:mysql://".length());
        int paramsStart = appUrl.indexOf("?", schemaStart);
        String params = paramsStart >= 0 ? appUrl.substring(paramsStart) : "";
        return appUrl.substring(0, schemaStart + 1) + params;
    }

    private String toSchemaUrl(String appUrl, String schema) {
        int schemaStart = appUrl.indexOf("/", "jdbc:mysql://".length());
        int paramsStart = appUrl.indexOf("?", schemaStart);
        String params = paramsStart >= 0 ? appUrl.substring(paramsStart) : "";
        return appUrl.substring(0, schemaStart + 1) + schema + params;
    }

    private boolean tableExists(Connection connection, String schema, String tableName) throws Exception {
        String sql = """
                SELECT COUNT(*)
                FROM information_schema.tables
                WHERE table_schema = ? AND table_name = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schema);
            statement.setString(2, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getInt(1) > 0;
            }
        }
    }

    private String latestForeignKeyError(String verifyUrl, String username, String password) {
        StringBuilder diagnostics = new StringBuilder();
        try (Connection connection = DriverManager.getConnection(verifyUrl, username, password);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SHOW ENGINE INNODB STATUS")) {
            if (resultSet.next()) {
                String status = resultSet.getString("Status");
                int marker = status.indexOf("LATEST FOREIGN KEY ERROR");
                diagnostics.append(marker >= 0 ? status.substring(marker, Math.min(status.length(), marker + 2000)) : status);
            }
        } catch (Exception ignored) {
            diagnostics.append("Migration failed and InnoDB status could not be read.");
        }
        diagnostics.append("\n\n--- SHOW CREATE TABLE diagnostics ---\n");
        for (String table : new String[]{"user", "order", "payment", "financial_ledger"}) {
            diagnostics.append(showCreateTable(verifyUrl, username, password, table)).append("\n");
        }
        return diagnostics.toString();
    }

    private String showCreateTable(String verifyUrl, String username, String password, String table) {
        try (Connection connection = DriverManager.getConnection(verifyUrl, username, password);
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery("SHOW CREATE TABLE `" + table + "`")) {
            if (resultSet.next()) {
                return resultSet.getString(2);
            }
        } catch (Exception ex) {
            return "Could not show table `" + table + "`: " + ex.getMessage();
        }
        return "No SHOW CREATE TABLE result for `" + table + "`.";
    }

    private Set<String> columns(Connection connection, String schema, String tableName) throws Exception {
        String sql = """
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = ? AND table_name = ?
                """;
        Set<String> values = new HashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schema);
            statement.setString(2, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    values.add(resultSet.getString(1));
                }
            }
        }
        return values;
    }

    private String columnType(Connection connection, String schema, String tableName, String columnName) throws Exception {
        String sql = """
                SELECT column_type
                FROM information_schema.columns
                WHERE table_schema = ? AND table_name = ? AND column_name = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schema);
            statement.setString(2, tableName);
            statement.setString(3, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                return resultSet.getString(1);
            }
        }
    }

    private boolean isGeneratedColumn(Connection connection, String schema, String tableName, String columnName) throws Exception {
        String sql = """
                SELECT extra
                FROM information_schema.columns
                WHERE table_schema = ? AND table_name = ? AND column_name = ?
                """;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schema);
            statement.setString(2, tableName);
            statement.setString(3, columnName);
            try (ResultSet resultSet = statement.executeQuery()) {
                assertThat(resultSet.next()).isTrue();
                return resultSet.getString(1).toLowerCase().contains("generated");
            }
        }
    }

    private Set<String> indexes(Connection connection, String schema, String tableName) throws Exception {
        String sql = """
                SELECT DISTINCT index_name
                FROM information_schema.statistics
                WHERE table_schema = ? AND table_name = ?
                """;
        Set<String> values = new HashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schema);
            statement.setString(2, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    values.add(resultSet.getString(1));
                }
            }
        }
        return values;
    }

    private Set<String> foreignKeys(Connection connection, String schema, String tableName) throws Exception {
        String sql = """
                SELECT constraint_name
                FROM information_schema.table_constraints
                WHERE table_schema = ? AND table_name = ? AND constraint_type = 'FOREIGN KEY'
                """;
        Set<String> values = new HashSet<>();
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, schema);
            statement.setString(2, tableName);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    values.add(resultSet.getString(1));
                }
            }
        }
        return values;
    }
}
