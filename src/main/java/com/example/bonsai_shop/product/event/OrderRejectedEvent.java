package com.example.bonsai_shop.product.event;

import com.example.bonsai_shop.entity.Order;

import lombok.Getter;

/**
 * [SỰ KIỆN TỪ CHỐI / HỦY ĐƠN HÀNG - DOMAIN EVENT]
 *
 * Được phát ra bởi:
 * - OrderService.rejectOrder(), OrderExpirationService.cancelSingleOrder(), hoặc markDepositedOrderCustomerNoShow().
 *
 * Được lắng nghe bởi:
 * - OrderEventListener.handleOrderRejectedEvent() (Phase AFTER_COMMIT, @Async) để gửi email thông báo hủy đơn kèm lý do cho khách hàng.
 */
@Getter
public class OrderRejectedEvent {
    private final Order order;
    private final String reason;

    public OrderRejectedEvent(Order order, String reason) {
        this.order = order;
        this.reason = reason;
    }
}
