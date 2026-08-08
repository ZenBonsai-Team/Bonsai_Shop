package com.example.bonsai_shop.integration.support;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;

public class TestDatabaseSafetyInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment env = applicationContext.getEnvironment();

        String datasourceUrl = env.getProperty("spring.datasource.url");
        TestDatabaseUrlValidator.requireExactTestSchema("spring.datasource.url", datasourceUrl);

        String flywayUrl = env.getProperty("spring.flyway.url");
        TestDatabaseUrlValidator.requireExactTestSchema("spring.flyway.url", flywayUrl);
    }
}
