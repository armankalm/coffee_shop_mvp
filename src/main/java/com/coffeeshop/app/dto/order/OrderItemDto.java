package com.coffeeshop.app.dto.order;

import com.coffeeshop.app.domain.OrderItem;
import com.coffeeshop.app.dto.product.ToppingDto;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

public class OrderItemDto {

    private Long id;
    private Long productId;
    private String productName;
    private Set<ToppingDto> toppings;
    private int quantity;
    private BigDecimal price;

    public static OrderItemDto from(OrderItem item) {
        OrderItemDto dto = new OrderItemDto();
        dto.id = item.getId();
        dto.productId = item.getProduct().getId();
        dto.productName = item.getProduct().getName();
        dto.toppings = item.getToppings().stream()
                .map(ToppingDto::fromWithoutIncompatibilities)
                .collect(Collectors.toSet());
        dto.quantity = item.getQuantity();
        dto.price = item.getPrice();
        return dto;
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public Set<ToppingDto> getToppings() { return toppings; }
    public int getQuantity() { return quantity; }
    public BigDecimal getPrice() { return price; }
}
