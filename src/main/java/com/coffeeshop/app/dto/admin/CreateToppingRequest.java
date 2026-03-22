package com.coffeeshop.app.dto.admin;

import com.coffeeshop.app.domain.ToppingType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

public class CreateToppingRequest {

    @NotBlank
    private String name;

    @NotNull
    private ToppingType type;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal price;

    private Set<Long> incompatibleWithIds = new HashSet<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public ToppingType getType() { return type; }
    public void setType(ToppingType type) { this.type = type; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Set<Long> getIncompatibleWithIds() { return incompatibleWithIds; }
    public void setIncompatibleWithIds(Set<Long> incompatibleWithIds) { this.incompatibleWithIds = incompatibleWithIds; }
}
