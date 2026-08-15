package com.example.bonsai_shop.product.scheduler;

import com.example.bonsai_shop.product.service.OrderExpirationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class OrderExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderExpirationScheduler.class);

    @Autowired
    private OrderExpirationService orderExpirationService;

    @Scheduled(fixedRate = 60000)
    public void scheduleOrderCleanup() {
        try {
            orderExpirationService.cancelExpiredOrders();
        } catch (Exception e) {
            log.error("Failed to auto-cancel expired orders.", e);
        }
    }

    @Scheduled(
            fixedDelayString = "${order.expiration.in-person-scan-ms:10000}",
            initialDelayString = "${order.expiration.in-person-scan-initial-delay-ms:10000}"
    )
    public void scheduleInPersonOrderCleanup() {
        try {
            orderExpirationService.cancelExpiredInPersonOrders();
        } catch (Exception e) {
            log.error("Failed to auto-cancel expired in-person orders.", e);
        }
    }
}
