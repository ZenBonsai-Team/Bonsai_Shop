package com.example.bonsai_shop.product.scheduler;

import com.example.bonsai_shop.product.service.OrderExpirationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * [SCHEDULER TỰ ĐỘNG QUÉT ĐƠN HÀNG HẾT HẠN ĐỊNH KỲ]
 *
 * Chịu trách nhiệm:
 * - Lên lịch thực thi các tác vụ chạy ngầm định kỳ (Background Cron/Scheduled Task) để quét và hủy các đơn hàng quá hạn thanh toán trong CSDL.
 *
 * Các tác vụ định kỳ:
 * 1. scheduleOrderCleanup(): Chạy mỗi 60 giây (fixedRate = 60000ms), quét đơn ONLINE quá hạn 15 phút và OFFLINE quá hạn 48h.
 * 2. scheduleInPersonOrderCleanup(): Chạy mỗi 10 giây (fixedDelayString = 10000ms), quét đơn IN_PERSON quá hạn.
 *
 * Các thành phần phối hợp chính:
 * - OrderExpirationService: Thực hiện logic hủy đơn, cập nhật payment EXPIRED, giải phóng Product về AVAILABLE.
 */
@Component
public class OrderExpirationScheduler {

    private static final Logger log = LoggerFactory.getLogger(OrderExpirationScheduler.class);

    @Autowired
    private OrderExpirationService orderExpirationService;

    /**
     * [TÁC VỤ ĐỊNH KỲ QUÉT ĐƠN HÀNG ONLINE/OFFLINE QUÁ HẠN (MỖI 60 GIÂY)]
     *
     * Chu kỳ chạy: Mỗi 60.000 ms (1 phút).
     *
     * Điều phối:
     * - Gọi OrderExpirationService.cancelExpiredOrders().
     */
    @Scheduled(fixedRate = 60000)
    public void scheduleOrderCleanup() {
        try {
            orderExpirationService.cancelExpiredOrders();
        } catch (Exception e) {
            log.error("Failed to auto-cancel expired orders.", e);
        }
    }

    /**
     * [TÁC VỤ ĐỊNH KỲ QUÉT ĐƠN HÀNG IN-PERSON QUÁ HẠN (MỖI 10 GIÂY)]
     *
     * Điều phối:
     * - Gọi OrderExpirationService.cancelExpiredInPersonOrders().
     */
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
