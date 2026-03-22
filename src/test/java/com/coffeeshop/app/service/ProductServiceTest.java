package com.coffeeshop.app.service;

import com.coffeeshop.app.domain.Product;
import com.coffeeshop.app.domain.ProductCategory;
import com.coffeeshop.app.dto.product.ProductDto;
import com.coffeeshop.app.repository.ProductRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    private Product buildProduct(Long id, String name, ProductCategory category) {
        return Product.builder()
                .id(id).name(name).category(category)
                .basePrice(BigDecimal.valueOf(500))
                .available(true)
                .build();
    }

    @Test
    void getAll_noCategory_returnsAllAvailable() {
        Product p1 = buildProduct(1L, "Latte", ProductCategory.COFFEE);
        Product p2 = buildProduct(2L, "Green Tea", ProductCategory.TEA);
        when(productRepository.findByAvailableTrue()).thenReturn(List.of(p1, p2));

        List<ProductDto> result = productService.getAll(null);

        assertThat(result).hasSize(2);
        verify(productRepository).findByAvailableTrue();
        verify(productRepository, never()).findByCategoryAndAvailableTrue(any());
    }

    @Test
    void getAll_withCategory_filtersCorrectly() {
        Product p1 = buildProduct(1L, "Latte", ProductCategory.COFFEE);
        when(productRepository.findByCategoryAndAvailableTrue(ProductCategory.COFFEE))
                .thenReturn(List.of(p1));

        List<ProductDto> result = productService.getAll(ProductCategory.COFFEE);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo(ProductCategory.COFFEE);
    }

    @Test
    void getById_existingId_returnsDto() {
        Product product = buildProduct(1L, "Espresso", ProductCategory.COFFEE);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        ProductDto result = productService.getById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Espresso");
    }

    @Test
    void getById_missingId_throwsNoSuchElement() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }
}
