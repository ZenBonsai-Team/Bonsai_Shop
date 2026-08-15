package com.example.bonsai_shop.product.event;

import com.example.bonsai_shop.entity.Order;
import lombok.Getter;

/**
 * [SỰ KIỆN TẠO ĐƠN HÀNG THÀNH CÔNG - DOMAIN EVENT]
 *
 * Được phát ra bởi:
 * - OrderService.createOrder() ngay sau khi đơn hàng được lưu vào DB.
 *
 * Được lắng nghe bởi:
 * - OrderEventListener.handleOrderCreatedEvent() (Phase AFTER_COMMIT, @Async) để gửi email tiếp nhận đơn cho khách.
 */
@Getter
public class OrderCreatedEvent {
    private final Order order;

    public OrderCreatedEvent(Order order) {
        this.order = order;
    }
}
