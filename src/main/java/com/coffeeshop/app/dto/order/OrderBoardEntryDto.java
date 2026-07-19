package com.coffeeshop.app.dto.order;

import com.coffeeshop.app.domain.Order;

/**
 * A single row on the customer-facing pickup board:
 * order number, customer name and current status.
 */
public class OrderBoardEntryDto {

    private Long orderId;
    private Integer dailyNumber;
    private String orderNumber;
    private String customerName;
    private String status;

    public static OrderBoardEntryDto from(Order order) {
        OrderBoardEntryDto dto = new OrderBoardEntryDto();
        dto.orderId = order.getId();
        // Prefer the per-shop daily number; fall back to the global id for legacy orders.
        Integer daily = order.getDailyNumber();
        dto.dailyNumber = daily;
        dto.orderNumber = "№" + (daily != null ? daily : order.getId());
        dto.customerName = OrderDto.resolveCustomerName(order);
        dto.status = order.getStatus().getCode();
        return dto;
    }

    public Long getOrderId() { return orderId; }
    public Integer getDailyNumber() { return dailyNumber; }
    public String getOrderNumber() { return orderNumber; }
    public String getCustomerName() { return customerName; }
    public String getStatus() { return status; }
}
