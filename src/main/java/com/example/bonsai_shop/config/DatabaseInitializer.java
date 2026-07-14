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
            System.out.println(">>> MIGRATING DATABASE VIA FLYWAY <<<");
            flyway.migrate();
            System.out.println(">>> FLYWAY MIGRATION SUCCESSFULLY COMPLETED <<<");
        } catch (Exception e) {
            System.err.println(">>> FLYWAY MIGRATION FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
