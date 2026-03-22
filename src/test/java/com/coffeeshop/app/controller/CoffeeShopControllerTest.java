package com.coffeeshop.app.controller;

import com.coffeeshop.app.domain.ShopStatus;
import com.coffeeshop.app.dto.shop.CoffeeShopDto;
import com.coffeeshop.app.service.CoffeeShopService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CoffeeShopController.class)
@Import({com.coffeeshop.app.config.SecurityConfig.class,
         com.coffeeshop.app.config.GlobalExceptionHandler.class,
         com.coffeeshop.app.security.JwtAuthFilter.class,
         com.coffeeshop.app.security.JwtTokenProvider.class,
         com.coffeeshop.app.security.JwtProperties.class})
class CoffeeShopControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CoffeeShopService coffeeShopService;

    private CoffeeShopDto buildDto(Long id, String name, String city) {
        CoffeeShopDto dto = new CoffeeShopDto();
        // Use reflection-free approach — rely on the static factory using a real CoffeeShop
        // We can't call the private constructor directly, so build via a helper
        return dto;
    }

    @Test
    @WithMockUser
    void getAll_returnsGroupedByCity() throws Exception {
        // Create DTO via static factory
        com.coffeeshop.app.domain.CoffeeShop shop = com.coffeeshop.app.domain.CoffeeShop.builder()
                .id(1L).name("Test Cafe").city("Almaty").address("Addr").status(ShopStatus.OPEN).build();
        CoffeeShopDto dto = CoffeeShopDto.from(shop);
        when(coffeeShopService.getAllGroupedByCity()).thenReturn(Map.of("Almaty", List.of(dto)));

        mockMvc.perform(get("/api/shops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.Almaty").isArray())
                .andExpect(jsonPath("$.Almaty[0].name").value("Test Cafe"));
    }

    @Test
    @WithMockUser
    void getById_existingId_returnsShop() throws Exception {
        com.coffeeshop.app.domain.CoffeeShop shop = com.coffeeshop.app.domain.CoffeeShop.builder()
                .id(1L).name("Test Cafe").city("Almaty").address("Addr").status(ShopStatus.OPEN).build();
        CoffeeShopDto dto = CoffeeShopDto.from(shop);
        when(coffeeShopService.getById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/shops/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.name").value("Test Cafe"));
    }

    @Test
    @WithMockUser
    void getById_notFound_returns404() throws Exception {
        when(coffeeShopService.getById(99L)).thenThrow(new NoSuchElementException("Coffee shop not found: 99"));

        mockMvc.perform(get("/api/shops/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Coffee shop not found: 99"));
    }

    @Test
    @WithMockUser
    void search_returnsMatchingShops() throws Exception {
        com.coffeeshop.app.domain.CoffeeShop shop = com.coffeeshop.app.domain.CoffeeShop.builder()
                .id(1L).name("Downtown Cafe").city("Almaty").address("Addr").status(ShopStatus.OPEN).build();
        when(coffeeShopService.search("downtown")).thenReturn(List.of(CoffeeShopDto.from(shop)));

        mockMvc.perform(get("/api/shops/search").param("query", "downtown"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Downtown Cafe"));
    }

    @Test
    void getAll_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/shops"))
                .andExpect(status().isUnauthorized());
    }
}
