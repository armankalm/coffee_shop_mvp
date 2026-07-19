package com.coffeeshop.app.dto.order;

import com.coffeeshop.app.domain.Order;

/**
 * A single row on the customer-facing pickup board:
 * order number, customer name and current status.
 */
public class OrderBoardEntryDto {

    private Long orderId;
    private String orderNumber;
    private String customerName;
    private String status;

    public static OrderBoardEntryDto from(Order order) {
        OrderBoardEntryDto dto = new OrderBoardEntryDto();
        dto.orderId = order.getId();
        dto.orderNumber = String.valueOf(order.getId());
        dto.customerName = OrderDto.resolveCustomerName(order);
        dto.status = order.getStatus().getCode();
        return dto;
    }

    public Long getOrderId() { return orderId; }
    public String getOrderNumber() { return orderNumber; }
    public String getCustomerName() { return customerName; }
    public String getStatus() { return status; }
}
