package com.coffeeshop.app.dto.product;

import com.coffeeshop.app.domain.Topping;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

public class ToppingDto {
    private Long id;
    private String name;
    private String type;
    private String typeNameRu;
    private BigDecimal price;
    private Set<Long> incompatibleWithIds;

    public ToppingDto() {}

    public static ToppingDto from(Topping topping) {
        ToppingDto dto = new ToppingDto();
        dto.id = topping.getId();
        dto.name = topping.getName();
        dto.type = topping.getType().getCode();
        dto.typeNameRu = topping.getType().getNameRu();
        dto.price = topping.getPrice();
        dto.incompatibleWithIds = topping.getIncompatibleWith().stream()
                .map(Topping::getId)
                .collect(Collectors.toSet());
        return dto;
    }

    /**
     * Lightweight mapping that skips the incompatibleWith collection,
     * avoiding a Cartesian product when used in order queries.
     */
    public static ToppingDto fromWithoutIncompatibilities(Topping topping) {
        ToppingDto dto = new ToppingDto();
        dto.id = topping.getId();
        dto.name = topping.getName();
        dto.type = topping.getType().getCode();
        dto.typeNameRu = topping.getType().getNameRu();
        dto.price = topping.getPrice();
        dto.incompatibleWithIds = Set.of();
        return dto;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getTypeNameRu() { return typeNameRu; }
    public BigDecimal getPrice() { return price; }
    public Set<Long> getIncompatibleWithIds() { return incompatibleWithIds; }
}
