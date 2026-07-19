package com.coffeeshop.app.service.board;

import org.springframework.context.ApplicationEvent;

/**
 * Published whenever an order changes: created, status changed directly, or its
 * items collectively reach a new status. Drives the live pickup board (per shop,
 * SSE) and per-order customer tracking (per order, SSE).
 */
public class OrderStatusChangedEvent extends ApplicationEvent {

    private final Long shopId;
    private final Long orderId;

    public OrderStatusChangedEvent(Object source, Long shopId, Long orderId) {
        super(source);
        this.shopId = shopId;
        this.orderId = orderId;
    }

    public Long getShopId() {
        return shopId;
    }

    public Long getOrderId() {
        return orderId;
    }
}
