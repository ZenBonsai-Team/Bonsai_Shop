package com.example.bonsai_shop.product.event;

import com.example.bonsai_shop.entity.Order;
import lombok.Getter;

/**
 * [SỰ KIỆN DUYỆT ĐƠN HÀNG THÀNH CÔNG - DOMAIN EVENT]
 *
 * Được phát ra bởi:
 * - OrderService.verifyOrder() sau khi Moderator duyệt phí và số tiền cọc.
 *
 * Được lắng nghe bởi:
 * - OrderEventListener.handleOrderVerifiedEvent() (Phase AFTER_COMMIT, @Async) để gửi email thông báo duyệt kèm link thanh toán VNPay.
 */
@Getter
public class OrderVerifiedEvent {
    private final Order order;

    public OrderVerifiedEvent(Order order) {
        this.order = order;
    }
}
