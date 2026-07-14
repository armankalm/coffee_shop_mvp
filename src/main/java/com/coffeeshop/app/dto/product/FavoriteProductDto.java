package com.coffeeshop.app.dto.product;

import com.coffeeshop.app.domain.FavoriteProduct;

public class FavoriteProductDto {

    private Long id;
    private ProductDto product;

    public static FavoriteProductDto from(FavoriteProduct favorite) {
        FavoriteProductDto dto = new FavoriteProductDto();
        dto.id = favorite.getId();
        dto.product = ProductDto.from(favorite.getProduct());
        return dto;
    }

    public Long getId() { return id; }
    public ProductDto getProduct() { return product; }
}
