package com.coffeeshop.app.controller;

import com.coffeeshop.app.dto.order.*;
import com.coffeeshop.app.service.SavedCombinationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class SavedCombinationController {

    private final SavedCombinationService savedCombinationService;

    public SavedCombinationController(SavedCombinationService savedCombinationService) {
        this.savedCombinationService = savedCombinationService;
    }

    @PostMapping("/api/saved-combinations")
    public ResponseEntity<SavedCombinationDto> saveCombination(
            Authentication authentication,
            @Valid @RequestBody SavedCombinationRequest request) {
        SavedCombinationDto dto = savedCombinationService.save(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/api/saved-combinations")
    public ResponseEntity<List<SavedCombinationDto>> getUserCombinations(
            Authentication authentication) {
        return ResponseEntity.ok(savedCombinationService.getUserCombinations(authentication.getName()));
    }

    @DeleteMapping("/api/saved-combinations/{id}")
    public ResponseEntity<Void> deleteCombination(
            Authentication authentication,
            @PathVariable("id") Long id) {
        savedCombinationService.delete(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/favorites")
    public ResponseEntity<FavoriteItemDto> addFavorite(
            Authentication authentication,
            @Valid @RequestBody AddFavoriteRequest request) {
        FavoriteItemDto dto = savedCombinationService.addFavorite(authentication.getName(), request.getSavedCombinationId());
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/api/favorites")
    public ResponseEntity<List<FavoriteItemDto>> getUserFavorites(
            Authentication authentication) {
        return ResponseEntity.ok(savedCombinationService.getUserFavorites(authentication.getName()));
    }

    @DeleteMapping("/api/favorites/{id}")
    public ResponseEntity<Void> removeFavorite(
            Authentication authentication,
            @PathVariable("id") Long id) {
        savedCombinationService.removeFavorite(authentication.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
