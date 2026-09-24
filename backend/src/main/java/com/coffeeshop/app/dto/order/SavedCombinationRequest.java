package com.coffeeshop.app.dto.order;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.HashSet;
import java.util.Set;

public class SavedCombinationRequest {

    @NotNull
    private Long productId;

    @NotBlank
    private String name;

    private Set<Long> toppingIds = new HashSet<>();

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Set<Long> getToppingIds() { return toppingIds; }
    public void setToppingIds(Set<Long> toppingIds) { this.toppingIds = toppingIds; }
}
