package com.coffeeshop.app.controller;

import com.coffeeshop.app.domain.Product;
import com.coffeeshop.app.domain.ProductCategory;
import com.coffeeshop.app.dto.product.ProductDto;
import com.coffeeshop.app.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@Import({com.coffeeshop.app.config.SecurityConfig.class,
         com.coffeeshop.app.config.GlobalExceptionHandler.class,
         com.coffeeshop.app.security.JwtAuthFilter.class,
         com.coffeeshop.app.security.JwtTokenProvider.class,
         com.coffeeshop.app.security.JwtProperties.class})
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    private ProductDto buildDto(Long id, String name, ProductCategory category) {
        Product product = Product.builder()
                .id(id).name(name).category(category)
                .basePrice(BigDecimal.valueOf(500))
                .available(true)
                .build();
        return ProductDto.from(product);
    }

    @Test
    @WithMockUser
    void getAll_noFilter_returnsAllProducts() throws Exception {
        ProductDto dto = buildDto(1L, "Latte", ProductCategory.COFFEE);
        when(productService.getAll(null)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Latte"));
    }

    @Test
    @WithMockUser
    void getAll_withCategoryFilter_filtersProducts() throws Exception {
        ProductDto dto = buildDto(1L, "Espresso", ProductCategory.COFFEE);
        when(productService.getAll(ProductCategory.COFFEE)).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/products").param("category", "COFFEE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].category").value("COFFEE"));
    }

    @Test
    @WithMockUser
    void getById_existingProduct_returnsProductWithToppings() throws Exception {
        ProductDto dto = buildDto(1L, "Cappuccino", ProductCategory.COFFEE);
        when(productService.getById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.availableToppings").isArray());
    }

    @Test
    @WithMockUser
    void getById_notFound_returns404() throws Exception {
        when(productService.getById(99L)).thenThrow(new NoSuchElementException("Product not found: 99"));

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound());
    }
}
