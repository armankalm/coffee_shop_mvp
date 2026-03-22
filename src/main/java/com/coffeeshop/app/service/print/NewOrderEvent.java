package com.coffeeshop.app.service.print;

import com.coffeeshop.app.domain.Order;
import org.springframework.context.ApplicationEvent;

public class NewOrderEvent extends ApplicationEvent {

    private final Order order;

    public NewOrderEvent(Object source, Order order) {
        super(source);
        this.order = order;
    }

    public Order getOrder() {
        return order;
    }
}
