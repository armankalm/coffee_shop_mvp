package com.coffeeshop.app.dto.order;

import com.coffeeshop.app.domain.OrderItem;

import java.time.Instant;
import java.util.stream.Collectors;

/**
 * Flat, kitchen-board oriented view of a single {@link OrderItem}.
 * One board card corresponds to one order item, which advances its status
 * independently of the other items in the same order.
 */
public class OrderItemBoardDto {

    private Long id;
    private Long orderId;
    private Long shopId;
    private Integer dailyNumber;
    private String orderNumber;
    private String title;
    private int quantity;
    private String status;
    private String statusNameRu;
    private Instant createdAt;
    private String comment;

    public static OrderItemBoardDto from(OrderItem item) {
        OrderItemBoardDto dto = new OrderItemBoardDto();
        dto.id = item.getId();
        dto.orderId = item.getOrder().getId();
        dto.shopId = item.getOrder().getShop().getId();
        // Prefer the per-shop daily number; fall back to the global id for legacy orders.
        Integer daily = item.getOrder().getDailyNumber();
        dto.dailyNumber = daily;
        dto.orderNumber = String.valueOf(daily != null ? daily : item.getOrder().getId());
        dto.title = item.getProduct().getName();
        dto.quantity = item.getQuantity();
        dto.status = item.getStatus().getCode();
        dto.statusNameRu = item.getStatus().getNameRu();
        dto.createdAt = item.getOrder().getCreatedAt();

        // The toppings selected for this item read naturally as a preparation note.
        String note = item.getToppings().stream()
                .map(topping -> topping.getName())
                .collect(Collectors.joining(", "));
        dto.comment = note.isBlank() ? null : note;

        return dto;
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public Long getShopId() { return shopId; }
    public Integer getDailyNumber() { return dailyNumber; }
    public String getOrderNumber() { return orderNumber; }
    public String getTitle() { return title; }
    public int getQuantity() { return quantity; }
    public String getStatus() { return status; }
    public String getStatusNameRu() { return statusNameRu; }
    public Instant getCreatedAt() { return createdAt; }
    public String getComment() { return comment; }
}
