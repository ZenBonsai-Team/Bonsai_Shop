package com.example.bonsai_shop.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class DatabaseInitializer implements CommandLineRunner {

    private final DataSource dataSource;

    public DatabaseInitializer(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println(">>> STARTING MANUAL FLYWAY MIGRATION <<<");
        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .baselineOnMigrate(true)
                    .baselineVersion("1")
                    .locations("classpath:db/migration")
                    .load();
            
            System.out.println(">>> REPAIRING FLYWAY SCHEMA HISTORY <<<");
            flyway.repair();
            
            // Check if V4 and V5 are applied. If not, drop duplicate columns to let Flyway migrate cleanly.
            try (java.sql.Connection conn = dataSource.getConnection()) {
                boolean hasHistoryTable = false;
                try (java.sql.ResultSet rs = conn.getMetaData().getTables(null, null, "flyway_schema_history", null)) {
                    if (rs.next()) {
                        hasHistoryTable = true;
                    }
                }
                
                boolean v4Applied = false;
                boolean v5Applied = false;
                if (hasHistoryTable) {
                    try (java.sql.PreparedStatement ps = conn.prepareStatement(
                            "SELECT COUNT(*) FROM flyway_schema_history WHERE version = ? AND success = 1")) {
                        ps.setString(1, "4");
                        try (java.sql.ResultSet rs = ps.executeQuery()) {
                            if (rs.next() && rs.getInt(1) > 0) {
                                v4Applied = true;
                            }
                        }
                        ps.setString(1, "5");
                        try (java.sql.ResultSet rs = ps.executeQuery()) {
                            if (rs.next() && rs.getInt(1) > 0) {
                                v5Applied = true;
                            }
                        }
                    }
                }
                
                if (!v4Applied || !v5Applied) {
                    try (java.sql.Statement stmt = conn.createStatement()) {
                        if (!v4Applied) {
                            try {
                                stmt.execute("ALTER TABLE community_comment DROP COLUMN ModerationReason");
                                System.out.println(">>> Dropped column ModerationReason from community_comment for Flyway sync");
                            } catch (java.sql.SQLException e) {
                                // Ignore if column doesn't exist
                            }
                        }
                        if (!v5Applied) {
                            try {
                                stmt.execute("ALTER TABLE community_comment DROP COLUMN Status");
                                System.out.println(">>> Dropped column Status from community_comment for Flyway sync");
                            } catch (java.sql.SQLException e) {
                                // Ignore if column doesn't exist
                            }
                        }
                    }
                }
            } catch (Exception e) {
                System.err.println(">>> Pre-migration check failed: " + e.getMessage());
            }

            System.out.println(">>> MIGRATING DATABASE VIA FLYWAY <<<");
            flyway.migrate();
            System.out.println(">>> FLYWAY MIGRATION SUCCESSFULLY COMPLETED <<<");
        } catch (Exception e) {
            System.err.println(">>> FLYWAY MIGRATION FAILED: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }
}
