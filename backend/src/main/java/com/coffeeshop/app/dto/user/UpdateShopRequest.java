package com.coffeeshop.app.dto.user;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public class UpdateShopRequest {

    @NotNull
    @Positive
    private Long shopId;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
}
