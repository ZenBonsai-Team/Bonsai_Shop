package com.example.bonsai_shop.integration.support;

import com.example.bonsai_shop.product.service.OrderExpirationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.config.ScheduledTaskHolder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.junit.jupiter.api.Assertions.*;

public class InfrastructureControllerContextSmokeTest extends BaseControllerIntegrationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private OrderExpirationService expirationService;

    @DisplayName("SMOKE-03: Controller Context Startup & MockMvc Request")
    @Test
    void testControllerContextMockMvc() throws Exception {
        assertNotNull(mockMvc);
        assertNotNull(datasourceUrl);
        assertTrue(datasourceUrl.contains("bonsai_shop_test"));

        mockMvc.perform(MockMvcRequestBuilders.get("/cart"))
                .andExpect(MockMvcResultMatchers.status().isOk());
    }

    @DisplayName("SMOKE-04: Scheduler Disabled Verification in Controller Context")
    @Test
    void testSchedulerDisabledInControllerContext() {
        assertTrue(Mockito.mockingDetails(expirationService).isMock(),
                "OrderExpirationService MUST be a Mockito mock in controller test context!");

        if (applicationContext.containsBean("scheduledTaskHolder")) {
            ScheduledTaskHolder taskHolder = applicationContext.getBean(ScheduledTaskHolder.class);
            boolean hasExpirationTask = taskHolder.getScheduledTasks().stream()
                    .anyMatch(task -> task.toString().contains("cancelExpiredOrders"));
            assertFalse(hasExpirationTask, "No scheduled task for cancelExpiredOrders should be registered!");
        }
    }
}
