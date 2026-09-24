package com.coffeeshop.app.dto.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Set;

public class CreateProductRequest {

    @NotNull
    private Long shopId;

    @NotBlank
    private String name;

    @NotBlank
    private String categoryCode;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal basePrice;

    private boolean available = true;

    private String description;

    private Set<Long> availableToppingIds;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategoryCode() { return categoryCode; }
    public void setCategoryCode(String categoryCode) { this.categoryCode = categoryCode; }

    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Set<Long> getAvailableToppingIds() { return availableToppingIds; }
    public void setAvailableToppingIds(Set<Long> availableToppingIds) { this.availableToppingIds = availableToppingIds; }
}
