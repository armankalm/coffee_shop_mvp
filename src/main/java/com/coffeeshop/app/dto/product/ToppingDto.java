package com.coffeeshop.app.dto.product;

import com.coffeeshop.app.domain.Topping;
import com.coffeeshop.app.domain.ToppingType;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

public class ToppingDto {
    private Long id;
    private String name;
    private ToppingType type;
    private BigDecimal price;
    private Set<Long> incompatibleWithIds;

    public ToppingDto() {}

    public static ToppingDto from(Topping topping) {
        ToppingDto dto = new ToppingDto();
        dto.id = topping.getId();
        dto.name = topping.getName();
        dto.type = topping.getType();
        dto.price = topping.getPrice();
        dto.incompatibleWithIds = topping.getIncompatibleWith().stream()
                .map(Topping::getId)
                .collect(Collectors.toSet());
        return dto;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public ToppingType getType() { return type; }
    public BigDecimal getPrice() { return price; }
    public Set<Long> getIncompatibleWithIds() { return incompatibleWithIds; }
}
