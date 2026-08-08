package com.example.bonsai_shop.integration.support;

import com.example.bonsai_shop.product.service.OrderExpirationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

public class InfrastructureSchedulerContextSmokeTest extends AbstractDatabaseSafeIntegrationTest {

    @Autowired
    private OrderExpirationService realExpirationService;

    @DisplayName("SMOKE-05: Scheduler Real Service Context Verification")
    @Test
    void testSchedulerRealServiceContext() {
        assertNotNull(realExpirationService);
        assertFalse(Mockito.mockingDetails(realExpirationService).isMock(),
                "OrderExpirationService MUST be a real Spring bean in scheduler test context!");
    }
}
