package com.coffeeshop.app.dto.admin;

import com.coffeeshop.app.domain.ProductCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.Set;

public class CreateProductRequest {

    @NotBlank
    private String name;

    @NotNull
    private ProductCategory category;

    @NotNull
    @DecimalMin("0.01")
    private BigDecimal basePrice;

    private boolean available = true;

    private Set<Long> availableToppingIds;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ProductCategory getCategory() { return category; }
    public void setCategory(ProductCategory category) { this.category = category; }

    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public Set<Long> getAvailableToppingIds() { return availableToppingIds; }
    public void setAvailableToppingIds(Set<Long> availableToppingIds) { this.availableToppingIds = availableToppingIds; }
}
