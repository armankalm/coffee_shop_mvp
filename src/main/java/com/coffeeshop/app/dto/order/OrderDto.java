package com.coffeeshop.app.dto.order;

import com.coffeeshop.app.domain.Order;
import com.coffeeshop.app.domain.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class OrderDto {

    private Long id;
    private Long userId;
    private Long shopId;
    private String shopName;
    private OrderStatus status;
    private BigDecimal total;
    private Instant createdAt;
    private List<OrderItemDto> items;

    public static OrderDto from(Order order) {
        OrderDto dto = new OrderDto();
        dto.id = order.getId();
        dto.userId = order.getUser().getId();
        dto.shopId = order.getShop().getId();
        dto.shopName = order.getShop().getName();
        dto.status = order.getStatus();
        dto.total = order.getTotal();
        dto.createdAt = order.getCreatedAt();
        dto.items = order.getItems().stream()
                .map(OrderItemDto::from)
                .collect(Collectors.toList());
        return dto;
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getShopId() { return shopId; }
    public String getShopName() { return shopName; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotal() { return total; }
    public Instant getCreatedAt() { return createdAt; }
    public List<OrderItemDto> getItems() { return items; }
}
