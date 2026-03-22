package com.coffeeshop.app.dto.order;

import com.coffeeshop.app.domain.SavedCombination;
import com.coffeeshop.app.dto.product.ToppingDto;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

public class SavedCombinationDto {

    private Long id;
    private Long productId;
    private String productName;
    private BigDecimal productBasePrice;
    private String name;
    private Set<ToppingDto> toppings;

    public static SavedCombinationDto from(SavedCombination sc) {
        SavedCombinationDto dto = new SavedCombinationDto();
        dto.id = sc.getId();
        dto.productId = sc.getProduct().getId();
        dto.productName = sc.getProduct().getName();
        dto.productBasePrice = sc.getProduct().getBasePrice();
        dto.name = sc.getName();
        dto.toppings = sc.getToppings().stream()
                .map(ToppingDto::from)
                .collect(Collectors.toSet());
        return dto;
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public BigDecimal getProductBasePrice() { return productBasePrice; }
    public String getName() { return name; }
    public Set<ToppingDto> getToppings() { return toppings; }
}
