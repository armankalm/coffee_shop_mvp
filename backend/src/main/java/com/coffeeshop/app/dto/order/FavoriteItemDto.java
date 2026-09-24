package com.coffeeshop.app.dto.order;

import com.coffeeshop.app.domain.FavoriteItem;

public class FavoriteItemDto {

    private Long id;
    private SavedCombinationDto savedCombination;

    public static FavoriteItemDto from(FavoriteItem item) {
        FavoriteItemDto dto = new FavoriteItemDto();
        dto.id = item.getId();
        dto.savedCombination = SavedCombinationDto.from(item.getSavedCombination());
        return dto;
    }

    public Long getId() { return id; }
    public SavedCombinationDto getSavedCombination() { return savedCombination; }
}
