package com.example.bonsai_shop.product.event;

import com.example.bonsai_shop.entity.Order;
import lombok.Getter;

/**
 * [SỰ KIỆN HOÀN TẤT THANH TOÁN 100% ĐƠN HÀNG - DOMAIN EVENT]
 *
 * Được phát ra bởi:
 * - OrderService.processPaymentSuccess() (khi thanh toán đủ qua VNPay) hoặc OrderService.confirmRemainingPayment() (khi thu nốt tiền mặt đợt 2).
 *
 * Được lắng nghe bởi:
 * - OrderEventListener.handleOrderPaidEvent() (Phase AFTER_COMMIT, @Async) để gửi email hóa đơn hoàn tất thanh toán cho khách hàng.
 */
@Getter
public class OrderPaidEvent {
    private final Order order;

    public OrderPaidEvent(Order order) {
        this.order = order;
    }
}
