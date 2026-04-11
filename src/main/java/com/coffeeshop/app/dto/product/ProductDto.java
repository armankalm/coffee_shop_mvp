package com.coffeeshop.app.dto.product;

import com.coffeeshop.app.domain.Product;

import java.math.BigDecimal;
import java.util.Set;
import java.util.stream.Collectors;

public class ProductDto {
    private Long id;
    private String name;
    private String category;
    private String categoryNameRu;
    private BigDecimal basePrice;
    private boolean available;
    private String imagePath;
    private Set<ToppingDto> availableToppings;

    public ProductDto() {}

    public static ProductDto from(Product product) {
        ProductDto dto = new ProductDto();
        dto.id = product.getId();
        dto.name = product.getName();
        dto.category = product.getCategory().getCode();
        dto.categoryNameRu = product.getCategory().getNameRu();
        dto.basePrice = product.getBasePrice();
        dto.available = product.isAvailable();
        dto.imagePath = product.getImagePath();
        dto.availableToppings = product.getAvailableToppings().stream()
                .map(ToppingDto::from)
                .collect(Collectors.toSet());
        return dto;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCategory() { return category; }
    public String getCategoryNameRu() { return categoryNameRu; }
    public BigDecimal getBasePrice() { return basePrice; }
    public boolean isAvailable() { return available; }
    public String getImagePath() { return imagePath; }
    public Set<ToppingDto> getAvailableToppings() { return availableToppings; }
}
