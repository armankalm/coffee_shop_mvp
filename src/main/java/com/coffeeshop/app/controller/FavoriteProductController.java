package com.coffeeshop.app.controller;

import com.coffeeshop.app.dto.product.FavoriteProductDto;
import com.coffeeshop.app.service.FavoriteProductService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/favorite-products")
public class FavoriteProductController {

    private final FavoriteProductService favoriteProductService;

    public FavoriteProductController(FavoriteProductService favoriteProductService) {
        this.favoriteProductService = favoriteProductService;
    }

    @GetMapping
    public ResponseEntity<List<FavoriteProductDto>> getUserFavorites(Authentication authentication) {
        return ResponseEntity.ok(favoriteProductService.getUserFavorites(authentication.getName()));
    }

    @PostMapping("/{productId}")
    public ResponseEntity<FavoriteProductDto> addFavorite(
            Authentication authentication,
            @PathVariable Long productId) {
        FavoriteProductDto dto = favoriteProductService.addFavorite(authentication.getName(), productId);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @DeleteMapping("/{productId}")
    public ResponseEntity<Void> removeFavorite(
            Authentication authentication,
            @PathVariable Long productId) {
        favoriteProductService.removeFavorite(authentication.getName(), productId);
        return ResponseEntity.noContent().build();
    }
}
