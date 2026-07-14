package com.example.bonsai_shop.product.event;

import com.example.bonsai_shop.entity.Order;
import lombok.Getter;

@Getter
public class OrderVerifiedEvent {
    private final Order order;

    public OrderVerifiedEvent(Order order) {
        this.order = order;
    }
}
