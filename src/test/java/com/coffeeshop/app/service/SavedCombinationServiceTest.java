package com.coffeeshop.app.service;

import com.coffeeshop.app.config.AccessDeniedException;
import com.coffeeshop.app.domain.*;
import com.coffeeshop.app.dto.order.*;
import com.coffeeshop.app.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SavedCombinationServiceTest {

    @Mock private SavedCombinationRepository savedCombinationRepository;
    @Mock private FavoriteItemRepository favoriteItemRepository;
    @Mock private UserRepository userRepository;
    @Mock private ProductRepository productRepository;
    @Mock private ToppingRepository toppingRepository;
    @Mock private ToppingService toppingService;

    @InjectMocks
    private SavedCombinationService savedCombinationService;

    private User user;
    private Product product;
    private SavedCombination combination;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("test@example.com").role(Role.USER).build();
        product = Product.builder().id(1L).name("Latte").category(ProductCategory.COFFEE)
                .basePrice(BigDecimal.valueOf(500)).available(true).build();
        combination = SavedCombination.builder()
                .id(1L).user(user).product(product).name("My Latte").build();
    }

    @Test
    void save_validRequest_returnsSavedCombinationDto() {
        SavedCombinationRequest request = new SavedCombinationRequest();
        request.setProductId(1L);
        request.setName("My Latte");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(savedCombinationRepository.save(any(SavedCombination.class))).thenReturn(combination);

        SavedCombinationDto result = savedCombinationService.save("test@example.com", request);

        assertThat(result).isNotNull();
        assertThat(result.getName()).isEqualTo("My Latte");
        assertThat(result.getProductName()).isEqualTo("Latte");
    }

    @Test
    void save_productNotFound_throwsNoSuchElement() {
        SavedCombinationRequest request = new SavedCombinationRequest();
        request.setProductId(99L);
        request.setName("Test");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> savedCombinationService.save("test@example.com", request))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    @Test
    void getUserCombinations_returnsUserCombinations() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(savedCombinationRepository.findByUserId(1L)).thenReturn(List.of(combination));

        List<SavedCombinationDto> result = savedCombinationService.getUserCombinations("test@example.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("My Latte");
    }

    @Test
    void delete_ownCombination_deletesSuccessfully() {
        when(savedCombinationRepository.findById(1L)).thenReturn(Optional.of(combination));
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));

        savedCombinationService.delete("test@example.com", 1L);

        verify(savedCombinationRepository).delete(combination);
    }

    @Test
    void delete_otherUsersCombination_throwsAccessDenied() {
        User otherUser = User.builder().id(2L).email("other@example.com").role(Role.USER).build();
        when(savedCombinationRepository.findById(1L)).thenReturn(Optional.of(combination));
        when(userRepository.findByEmail("other@example.com")).thenReturn(Optional.of(otherUser));

        assertThatThrownBy(() -> savedCombinationService.delete("other@example.com", 1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void addFavorite_newFavorite_returnsFavoriteItemDto() {
        FavoriteItem favorite = FavoriteItem.builder()
                .id(1L).user(user).savedCombination(combination).build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(savedCombinationRepository.findById(1L)).thenReturn(Optional.of(combination));
        when(favoriteItemRepository.existsByUserIdAndSavedCombinationId(1L, 1L)).thenReturn(false);
        when(favoriteItemRepository.save(any(FavoriteItem.class))).thenReturn(favorite);

        FavoriteItemDto result = savedCombinationService.addFavorite("test@example.com", 1L);

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void addFavorite_alreadyFavorite_throwsIllegalArgument() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(savedCombinationRepository.findById(1L)).thenReturn(Optional.of(combination));
        when(favoriteItemRepository.existsByUserIdAndSavedCombinationId(1L, 1L)).thenReturn(true);

        assertThatThrownBy(() -> savedCombinationService.addFavorite("test@example.com", 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Already in favorites");
    }

    @Test
    void getUserFavorites_returnsUserFavorites() {
        FavoriteItem favorite = FavoriteItem.builder()
                .id(1L).user(user).savedCombination(combination).build();

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(user));
        when(favoriteItemRepository.findByUserId(1L)).thenReturn(List.of(favorite));

        List<FavoriteItemDto> result = savedCombinationService.getUserFavorites("test@example.com");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSavedCombination().getName()).isEqualTo("My Latte");
    }
}
