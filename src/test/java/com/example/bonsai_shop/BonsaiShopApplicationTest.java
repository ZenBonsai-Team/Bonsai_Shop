package com.example.bonsai_shop;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import static org.junit.jupiter.api.Assertions.*;

@DirtiesContext
class BonsaiShopApplicationTest {

    @Test
    void testBonsaiShopApplicationConstructor() {
        BonsaiShopApplication app = new BonsaiShopApplication();
        assertNotNull(app);
    }

    @Test
    void testBonsaiShopApplicationMain() {
        System.setProperty("spring.profiles.active", "test");
        assertDoesNotThrow(() -> {
            try (ConfigurableApplicationContext context = SpringApplication.run(BonsaiShopApplication.class, "--server.port=0")) {
                assertNotNull(context);
            }
        });
    }
}
