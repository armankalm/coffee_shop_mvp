package com.coffeeshop.app.dto.order;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.HashSet;
import java.util.Set;

public class OrderItemRequest {

    @NotNull
    private Long productId;

    private Set<Long> toppingIds = new HashSet<>();

    @Min(1)
    @Max(100)
    private int quantity = 1;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public Set<Long> getToppingIds() { return toppingIds; }
    public void setToppingIds(Set<Long> toppingIds) { this.toppingIds = toppingIds; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
}
