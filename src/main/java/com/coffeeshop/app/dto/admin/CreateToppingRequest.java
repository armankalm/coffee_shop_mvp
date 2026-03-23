package com.coffeeshop.app.dto.admin;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;

public class CreateToppingRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String typeCode;

    @NotNull
    @DecimalMin("0.00")
    private BigDecimal price;

    private Set<Long> incompatibleWithIds = new HashSet<>();

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getTypeCode() { return typeCode; }
    public void setTypeCode(String typeCode) { this.typeCode = typeCode; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public Set<Long> getIncompatibleWithIds() { return incompatibleWithIds; }
    public void setIncompatibleWithIds(Set<Long> incompatibleWithIds) { this.incompatibleWithIds = incompatibleWithIds; }
}
