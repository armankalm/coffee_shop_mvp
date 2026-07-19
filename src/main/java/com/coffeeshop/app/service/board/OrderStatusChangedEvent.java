package com.coffeeshop.app.service.board;

import org.springframework.context.ApplicationEvent;

/**
 * Published whenever an order's status changes (directly or as a side effect
 * of all its items reaching a new status). Drives the live pickup board (SSE).
 */
public class OrderStatusChangedEvent extends ApplicationEvent {

    private final Long shopId;

    public OrderStatusChangedEvent(Object source, Long shopId) {
        super(source);
        this.shopId = shopId;
    }

    public Long getShopId() {
        return shopId;
    }
}
