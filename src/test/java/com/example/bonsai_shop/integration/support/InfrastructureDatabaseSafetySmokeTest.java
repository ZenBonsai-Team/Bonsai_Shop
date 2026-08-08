package com.example.bonsai_shop.integration.support;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.mock.env.MockEnvironment;

import static org.junit.jupiter.api.Assertions.*;

public class InfrastructureDatabaseSafetySmokeTest {

    @DisplayName("SMOKE-01: Negative Safety Guard Test - Unsafe schema MUST fail before context refresh")
    @Test
    void testUnsafeDatabaseConfigurationIsRejected() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("spring.datasource.url", "jdbc:mysql://localhost:3306/bonsai_shop"); // Unsafe prod/dev schema
        env.setProperty("spring.flyway.url", "jdbc:mysql://localhost:3306/bonsai_shop");

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.setEnvironment(env);

        TestDatabaseSafetyInitializer initializer = new TestDatabaseSafetyInitializer();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            initializer.initialize(context);
        });

        assertTrue(exception.getMessage().contains("MUST target exact schema 'bonsai_shop_test'"));
    }

    @DisplayName("SMOKE-02: Missing Flyway Property Test - Missing spring.flyway.url MUST fail")
    @Test
    void testMissingFlywayPropertyIsRejected() {
        MockEnvironment env = new MockEnvironment();
        env.setProperty("spring.datasource.url", "jdbc:mysql://localhost:3306/bonsai_shop_test");

        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.setEnvironment(env);

        TestDatabaseSafetyInitializer initializer = new TestDatabaseSafetyInitializer();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            initializer.initialize(context);
        });

        assertTrue(exception.getMessage().contains("spring.flyway.url"));
    }
}
