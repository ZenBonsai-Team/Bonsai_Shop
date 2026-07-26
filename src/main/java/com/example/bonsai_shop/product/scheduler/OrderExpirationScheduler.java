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

    // Chạy tự động kiểm tra định kỳ mỗi 1 phút (60,000 milliseconds)
    @Scheduled(fixedRate = 60000)
    public void scheduleOrderCleanup() {
        try {
            orderExpirationService.cancelExpiredOrders();
        } catch (Exception e) {
            log.error("Lỗi khi chạy tác vụ tự động hủy đơn hàng quá hạn: ", e);
        }
    }
}
