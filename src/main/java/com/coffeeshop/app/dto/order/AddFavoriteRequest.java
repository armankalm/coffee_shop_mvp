package com.coffeeshop.app.dto.order;

import jakarta.validation.constraints.NotNull;

public class AddFavoriteRequest {

    @NotNull
    private Long savedCombinationId;

    public Long getSavedCombinationId() { return savedCombinationId; }
    public void setSavedCombinationId(Long savedCombinationId) { this.savedCombinationId = savedCombinationId; }
}
