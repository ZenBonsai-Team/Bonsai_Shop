package com.example.bonsai_shop.integration.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Value;

@DatabaseSafeIntegrationTest
public abstract class AbstractDatabaseSafeIntegrationTest {

    @Value("${spring.datasource.url:}")
    protected String datasourceUrl;

    @BeforeEach
    void defenseInDepthSchemaCheck() {
        TestDatabaseUrlValidator.requireExactTestSchema("spring.datasource.url", datasourceUrl);
    }
}
