package com.example.bonsai_shop.product.event;

import com.example.bonsai_shop.entity.Order;

import lombok.Getter;

@Getter
public class OrderRejectedEvent {
    private final Order order;
    private final String reason;

    public OrderRejectedEvent(Order order, String reason) {
        this.order = order;
        this.reason = reason;
    }
}
