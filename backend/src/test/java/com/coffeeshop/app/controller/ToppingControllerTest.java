package com.coffeeshop.app.controller;

import com.coffeeshop.app.domain.RefToppingType;
import com.coffeeshop.app.domain.Topping;
import com.coffeeshop.app.dto.product.ToppingDto;
import com.coffeeshop.app.service.ToppingService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ToppingController.class)
@Import({com.coffeeshop.app.config.SecurityConfig.class,
         com.coffeeshop.app.config.GlobalExceptionHandler.class,
         com.coffeeshop.app.security.JwtAuthFilter.class,
         com.coffeeshop.app.security.JwtTokenProvider.class,
         com.coffeeshop.app.security.JwtProperties.class})
class ToppingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ToppingService toppingService;

    private RefToppingType milkType() {
        return RefToppingType.builder().id(1L).code("MILK").nameRu("Молоко").nameEn("Milk").build();
    }

    private RefToppingType syrupType() {
        return RefToppingType.builder().id(2L).code("SYRUP").nameRu("Сиропы").nameEn("Syrup").build();
    }

    private ToppingDto buildDto(Long id, String name, RefToppingType type) {
        Topping topping = Topping.builder()
                .id(id).name(name).type(type)
                .price(BigDecimal.valueOf(100))
                .build();
        return ToppingDto.from(topping);
    }

    @Test
    @WithMockUser
    void getAll_returnsGroupedByType() throws Exception {
        ToppingDto milk = buildDto(1L, "Oat Milk", milkType());
        ToppingDto syrup = buildDto(2L, "Vanilla Syrup", syrupType());
        when(toppingService.getAllGroupedByType()).thenReturn(
                Map.of("MILK", List.of(milk),
                       "SYRUP", List.of(syrup)));

        mockMvc.perform(get("/api/toppings"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.MILK").isArray())
                .andExpect(jsonPath("$.SYRUP").isArray());
    }

    @Test
    void getAll_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/toppings"))
                .andExpect(status().isUnauthorized());
    }
}
