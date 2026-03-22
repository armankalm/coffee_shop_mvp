package com.coffeeshop.app.dto.product;

import com.coffeeshop.app.domain.Product;
import com.coffeeshop.app.domain.ProductCategory;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

public class ProductDto {
    private Long id;
    private String name;
    private ProductCategory category;
    private BigDecimal basePrice;
    private boolean available;
    private Set<ToppingDto> availableToppings;

    public ProductDto() {}

    public static ProductDto from(Product product) {
        ProductDto dto = new ProductDto();
        dto.id = product.getId();
        dto.name = product.getName();
        dto.category = product.getCategory();
        dto.basePrice = product.getBasePrice();
        dto.available = product.isAvailable();
        dto.availableToppings = product.getAvailableToppings().stream()
                .map(ToppingDto::from)
                .collect(Collectors.toSet());
        return dto;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public ProductCategory getCategory() { return category; }
    public BigDecimal getBasePrice() { return basePrice; }
    public boolean isAvailable() { return available; }
    public Set<ToppingDto> getAvailableToppings() { return availableToppings; }
}
