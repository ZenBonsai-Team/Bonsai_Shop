package com.example.bonsai_shop.integration.support;

import java.net.URI;

public class TestDatabaseUrlValidator {

    public static void requireExactTestSchema(String propertyName, String jdbcUrl) {
        if (jdbcUrl == null || jdbcUrl.isBlank()) {
            throw new IllegalStateException("CRITICAL DATABASE SAFETY GUARD: Property '" + propertyName + "' is missing or empty!");
        }
        if (!jdbcUrl.startsWith("jdbc:mysql://")) {
            throw new IllegalArgumentException("CRITICAL DATABASE SAFETY GUARD: Unsupported JDBC URL format for property '" + propertyName + "'.");
        }

        String cleanUrl = jdbcUrl.substring(5); // Strip 'jdbc:'
        URI uri = URI.create(cleanUrl);
        String path = uri.getPath();
        String schema = (path != null && path.startsWith("/")) ? path.substring(1) : path;

        if (schema != null && schema.contains("?")) {
            schema = schema.substring(0, schema.indexOf("?"));
        }

        if (!"bonsai_shop_test".equals(schema)) {
            String redactedUrl = jdbcUrl.contains("?") ? jdbcUrl.substring(0, jdbcUrl.indexOf("?")) : jdbcUrl;
            throw new IllegalStateException(
                "CRITICAL DATABASE SAFETY GUARD TRIGGERED: Property '" + propertyName + "' MUST target exact schema 'bonsai_shop_test'! " +
                "Actual parsed schema: '" + schema + "' (Redacted URL: " + redactedUrl + ")"
            );
        }
    }
}
