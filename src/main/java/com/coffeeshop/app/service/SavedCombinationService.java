package com.coffeeshop.app.service;

import com.coffeeshop.app.domain.*;
import com.coffeeshop.app.dto.order.FavoriteItemDto;
import com.coffeeshop.app.dto.order.SavedCombinationDto;
import com.coffeeshop.app.dto.order.SavedCombinationRequest;
import com.coffeeshop.app.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Transactional
public class SavedCombinationService {

    private final SavedCombinationRepository savedCombinationRepository;
    private final FavoriteItemRepository favoriteItemRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ToppingRepository toppingRepository;
    private final ToppingService toppingService;

    public SavedCombinationService(SavedCombinationRepository savedCombinationRepository,
                                   FavoriteItemRepository favoriteItemRepository,
                                   UserRepository userRepository,
                                   ProductRepository productRepository,
                                   ToppingRepository toppingRepository,
                                   ToppingService toppingService) {
        this.savedCombinationRepository = savedCombinationRepository;
        this.favoriteItemRepository = favoriteItemRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.toppingRepository = toppingRepository;
        this.toppingService = toppingService;
    }

    public SavedCombinationDto save(String userEmail, SavedCombinationRequest request) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userEmail));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + request.getProductId()));

        Set<Long> toppingIds = request.getToppingIds() != null ? request.getToppingIds() : new HashSet<>();
        toppingService.validateCompatibility(toppingIds);

        Set<Topping> toppings = new HashSet<>();
        if (!toppingIds.isEmpty()) {
            toppings = new HashSet<>(toppingRepository.findAllById(toppingIds));
            if (toppings.size() != toppingIds.size()) {
                throw new IllegalArgumentException("One or more toppings not found");
            }
            Set<Long> allowedIds = product.getAvailableToppings().stream()
                    .map(Topping::getId)
                    .collect(Collectors.toSet());
            for (Topping t : toppings) {
                if (!allowedIds.contains(t.getId())) {
                    throw new IllegalArgumentException(
                            "Topping '" + t.getName() + "' is not available for product '" + product.getName() + "'");
                }
            }
        }

        SavedCombination combination = SavedCombination.builder()
                .user(user)
                .product(product)
                .name(request.getName())
                .toppings(toppings)
                .build();

        return SavedCombinationDto.from(savedCombinationRepository.save(combination));
    }

    @Transactional(readOnly = true)
    public List<SavedCombinationDto> getUserCombinations(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userEmail));
        return savedCombinationRepository.findByUserId(user.getId()).stream()
                .map(SavedCombinationDto::from)
                .collect(Collectors.toList());
    }

    public void delete(String userEmail, Long combinationId) {
        SavedCombination combination = savedCombinationRepository.findById(combinationId)
                .orElseThrow(() -> new NoSuchElementException("Saved combination not found: " + combinationId));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userEmail));

        if (!combination.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Access denied to saved combination: " + combinationId);
        }

        savedCombinationRepository.delete(combination);
    }

    public FavoriteItemDto addFavorite(String userEmail, Long savedCombinationId) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userEmail));

        SavedCombination combination = savedCombinationRepository.findById(savedCombinationId)
                .orElseThrow(() -> new NoSuchElementException("Saved combination not found: " + savedCombinationId));

        if (!combination.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Access denied to saved combination: " + savedCombinationId);
        }

        if (favoriteItemRepository.existsByUserIdAndSavedCombinationId(user.getId(), savedCombinationId)) {
            throw new IllegalArgumentException("Already in favorites: " + savedCombinationId);
        }

        FavoriteItem favorite = FavoriteItem.builder()
                .user(user)
                .savedCombination(combination)
                .build();

        return FavoriteItemDto.from(favoriteItemRepository.save(favorite));
    }

    @Transactional(readOnly = true)
    public List<FavoriteItemDto> getUserFavorites(String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userEmail));
        return favoriteItemRepository.findByUserId(user.getId()).stream()
                .map(FavoriteItemDto::from)
                .collect(Collectors.toList());
    }

    public void removeFavorite(String userEmail, Long favoriteItemId) {
        FavoriteItem favorite = favoriteItemRepository.findById(favoriteItemId)
                .orElseThrow(() -> new NoSuchElementException("Favorite not found: " + favoriteItemId));

        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userEmail));

        if (!favorite.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("Access denied to favorite: " + favoriteItemId);
        }

        favoriteItemRepository.delete(favorite);
    }
}
