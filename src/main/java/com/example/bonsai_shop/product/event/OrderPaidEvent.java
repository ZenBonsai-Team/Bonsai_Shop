package com.example.bonsai_shop.product.event;

import com.example.bonsai_shop.entity.Order;
import lombok.Getter;

@Getter
public class OrderPaidEvent {
    private final Order order;

    public OrderPaidEvent(Order order) {
        this.order = order;
    }
}
