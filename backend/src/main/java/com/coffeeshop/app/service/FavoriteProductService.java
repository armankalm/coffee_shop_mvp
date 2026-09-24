package com.coffeeshop.app.service;

import com.coffeeshop.app.config.AccessDeniedException;
import com.coffeeshop.app.domain.FavoriteProduct;
import com.coffeeshop.app.domain.Product;
import com.coffeeshop.app.domain.User;
import com.coffeeshop.app.dto.product.FavoriteProductDto;
import com.coffeeshop.app.repository.FavoriteProductRepository;
import com.coffeeshop.app.repository.ProductRepository;
import com.coffeeshop.app.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@Transactional
public class FavoriteProductService {

    private final FavoriteProductRepository favoriteProductRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public FavoriteProductService(FavoriteProductRepository favoriteProductRepository,
                                   ProductRepository productRepository,
                                   UserRepository userRepository) {
        this.favoriteProductRepository = favoriteProductRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<FavoriteProductDto> getUserFavorites(String userEmail) {
        User user = resolveUser(userEmail);
        return favoriteProductRepository.findByUserId(user.getId()).stream()
                .map(FavoriteProductDto::from)
                .collect(Collectors.toList());
    }

    public FavoriteProductDto addFavorite(String userEmail, Long productId) {
        User user = resolveUser(userEmail);
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + productId));
        if (favoriteProductRepository.existsByUserIdAndProductId(user.getId(), productId)) {
            throw new IllegalArgumentException("Already in favorites: " + productId);
        }
        FavoriteProduct favorite = FavoriteProduct.builder().user(user).product(product).build();
        return FavoriteProductDto.from(favoriteProductRepository.save(favorite));
    }

    public void removeFavorite(String userEmail, Long productId) {
        User user = resolveUser(userEmail);
        FavoriteProduct favorite = favoriteProductRepository.findByUserIdAndProductId(user.getId(), productId)
                .orElseThrow(() -> new NoSuchElementException("Favorite not found for product: " + productId));
        if (!favorite.getUser().getId().equals(user.getId())) {
            throw new AccessDeniedException("Access denied to favorite: " + productId);
        }
        favoriteProductRepository.delete(favorite);
    }

    private User resolveUser(String userEmail) {
        return userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userEmail));
    }
}
