package com.coffeeshop.app.controller;

import com.coffeeshop.app.domain.Product;
import com.coffeeshop.app.domain.RefProductCategory;
import com.coffeeshop.app.dto.product.ProductDto;
import com.coffeeshop.app.service.ProductService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductImageController.class)
@Import({com.coffeeshop.app.config.SecurityConfig.class,
         com.coffeeshop.app.config.GlobalExceptionHandler.class,
         com.coffeeshop.app.security.JwtAuthFilter.class,
         com.coffeeshop.app.security.JwtTokenProvider.class,
         com.coffeeshop.app.security.JwtProperties.class})
class ProductImageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    private ProductDto buildDtoWithImage() {
        RefProductCategory category = RefProductCategory.builder()
                .id(1L).code("COFFEE").nameRu("Кофе").nameEn("Coffee").build();
        Product product = Product.builder()
                .id(1L).name("Latte").category(category)
                .basePrice(BigDecimal.valueOf(500))
                .available(true)
                .imagePath("/uploads/products/abc.jpg")
                .build();
        return ProductDto.from(product);
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void uploadImage_asManager_returnsUpdatedProduct() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "fake-image".getBytes());
        when(productService.updateImage(eq(1L), any())).thenReturn(buildDtoWithImage());

        mockMvc.perform(multipart("/api/products/1/image").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.imagePath").value("/uploads/products/abc.jpg"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void uploadImage_asAdmin_returnsUpdatedProduct() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "fake-image".getBytes());
        when(productService.updateImage(eq(1L), any())).thenReturn(buildDtoWithImage());

        mockMvc.perform(multipart("/api/products/1/image").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(roles = "USER")
    void uploadImage_asUser_returns403() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "fake-image".getBytes());

        mockMvc.perform(multipart("/api/products/1/image").file(file))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void uploadImage_productNotFound_returns404() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "photo.jpg", "image/jpeg", "fake-image".getBytes());
        when(productService.updateImage(eq(99L), any()))
                .thenThrow(new NoSuchElementException("Product not found: 99"));

        mockMvc.perform(multipart("/api/products/99/image").file(file))
                .andExpect(status().isNotFound());
    }
}
