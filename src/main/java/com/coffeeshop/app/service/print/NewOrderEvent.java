package com.coffeeshop.app.service.print;

import org.springframework.context.ApplicationEvent;

public class NewOrderEvent extends ApplicationEvent {

    private final Long orderId;

    public NewOrderEvent(Object source, Long orderId) {
        super(source);
        this.orderId = orderId;
    }

    public Long getOrderId() {
        return orderId;
    }
}
