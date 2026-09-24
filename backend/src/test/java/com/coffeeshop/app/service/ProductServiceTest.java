package com.coffeeshop.app.service;

import com.coffeeshop.app.domain.Product;
import com.coffeeshop.app.domain.RefProductCategory;
import com.coffeeshop.app.dto.product.ProductDto;
import com.coffeeshop.app.repository.ProductRepository;
import com.coffeeshop.app.repository.RefProductCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private RefProductCategoryRepository refProductCategoryRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private ProductService productService;

    private RefProductCategory coffeeCategory;
    private RefProductCategory teaCategory;

    private static final Long SHOP_ID = 1L;

    @BeforeEach
    void setUp() {
        coffeeCategory = RefProductCategory.builder().id(1L).code("COFFEE").nameRu("Кофе").nameEn("Coffee").build();
        teaCategory = RefProductCategory.builder().id(2L).code("TEA").nameRu("Чай").nameEn("Tea").build();
    }

    private Product buildProduct(Long id, String name, RefProductCategory category) {
        return Product.builder()
                .id(id).name(name).category(category)
                .basePrice(BigDecimal.valueOf(500))
                .available(true)
                .build();
    }

    @Test
    void getAll_noCategory_returnsAllAvailable() {
        Product p1 = buildProduct(1L, "Latte", coffeeCategory);
        Product p2 = buildProduct(2L, "Green Tea", teaCategory);
        when(productRepository.findAllAvailableWithToppingsByShop(SHOP_ID)).thenReturn(List.of(p1, p2));

        List<ProductDto> result = productService.getAll(SHOP_ID, null);

        assertThat(result).hasSize(2);
        verify(productRepository).findAllAvailableWithToppingsByShop(SHOP_ID);
        verify(productRepository, never()).findByCategoryAndAvailableTrueWithToppingsByShop(any(), any());
    }

    @Test
    void getAll_withCategory_filtersCorrectly() {
        Product p1 = buildProduct(1L, "Latte", coffeeCategory);
        when(refProductCategoryRepository.findByCode("COFFEE")).thenReturn(Optional.of(coffeeCategory));
        when(productRepository.findByCategoryAndAvailableTrueWithToppingsByShop(coffeeCategory, SHOP_ID)).thenReturn(List.of(p1));

        List<ProductDto> result = productService.getAll(SHOP_ID, "COFFEE");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo("COFFEE");
    }

    @Test
    void getById_existingId_returnsDto() {
        Product product = buildProduct(1L, "Espresso", coffeeCategory);
        when(productRepository.findByIdWithToppings(1L)).thenReturn(Optional.of(product));

        ProductDto result = productService.getById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Espresso");
    }

    @Test
    void getById_missingId_throwsNoSuchElement() {
        when(productRepository.findByIdWithToppings(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateImage_existingProduct_updatesImagePath() throws IOException {
        Product product = buildProduct(1L, "Latte", coffeeCategory);
        when(productRepository.findByIdWithToppings(1L)).thenReturn(Optional.of(product));
        when(fileStorageService.store(any())).thenReturn("/uploads/products/abc.jpg");
        when(productRepository.save(any(Product.class))).thenReturn(product);

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "data".getBytes());

        ProductDto result = productService.updateImage(1L, file);

        assertThat(result.getImagePath()).isEqualTo("/uploads/products/abc.jpg");
        verify(productRepository).save(product);
    }

    @Test
    void updateImage_missingProduct_throwsNoSuchElement() {
        when(productRepository.findByIdWithToppings(99L)).thenReturn(Optional.empty());

        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "data".getBytes());

        assertThatThrownBy(() -> productService.updateImage(99L, file))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }
}
