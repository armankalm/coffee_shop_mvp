package com.coffeeshop.app.controller;

import com.coffeeshop.app.domain.*;
import com.coffeeshop.app.dto.order.*;
import com.coffeeshop.app.service.SavedCombinationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SavedCombinationController.class)
@Import({com.coffeeshop.app.config.SecurityConfig.class,
         com.coffeeshop.app.config.GlobalExceptionHandler.class,
         com.coffeeshop.app.security.JwtAuthFilter.class,
         com.coffeeshop.app.security.JwtTokenProvider.class,
         com.coffeeshop.app.security.JwtProperties.class})
class SavedCombinationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SavedCombinationService savedCombinationService;

    private RefUserRole userRole() {
        return RefUserRole.builder().id(1L).code("USER").nameRu("Пользователь").nameEn("User").build();
    }

    private RefProductCategory coffeeCategory() {
        return RefProductCategory.builder().id(1L).code("COFFEE").nameRu("Кофе").nameEn("Coffee").build();
    }

    private SavedCombinationDto buildCombinationDto() {
        User user = User.builder().id(1L).email("user@test.com").role(userRole()).build();
        Product product = Product.builder().id(1L).name("Latte")
                .category(coffeeCategory()).basePrice(BigDecimal.valueOf(500)).available(true).build();
        SavedCombination sc = SavedCombination.builder()
                .id(1L).user(user).product(product).name("My Latte").build();
        return SavedCombinationDto.from(sc);
    }

    private FavoriteItemDto buildFavoriteItemDto() {
        User user = User.builder().id(1L).email("user@test.com").role(userRole()).build();
        Product product = Product.builder().id(1L).name("Latte")
                .category(coffeeCategory()).basePrice(BigDecimal.valueOf(500)).available(true).build();
        SavedCombination sc = SavedCombination.builder()
                .id(1L).user(user).product(product).name("My Latte").build();
        FavoriteItem fi = FavoriteItem.builder().id(1L).user(user).savedCombination(sc).build();
        return FavoriteItemDto.from(fi);
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void saveCombination_validRequest_returns201() throws Exception {
        SavedCombinationRequest request = new SavedCombinationRequest();
        request.setProductId(1L);
        request.setName("My Latte");

        SavedCombinationDto dto = buildCombinationDto();
        when(savedCombinationService.save(eq("user@test.com"), any(SavedCombinationRequest.class)))
                .thenReturn(dto);

        mockMvc.perform(post("/api/saved-combinations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("My Latte"))
                .andExpect(jsonPath("$.productName").value("Latte"));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void getUserCombinations_returnsList() throws Exception {
        SavedCombinationDto dto = buildCombinationDto();
        when(savedCombinationService.getUserCombinations("user@test.com")).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/saved-combinations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("My Latte"));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void deleteCombination_returnsNoContent() throws Exception {
        doNothing().when(savedCombinationService).delete("user@test.com", 1L);

        mockMvc.perform(delete("/api/saved-combinations/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void addFavorite_validRequest_returns201() throws Exception {
        AddFavoriteRequest request = new AddFavoriteRequest();
        request.setSavedCombinationId(1L);

        FavoriteItemDto dto = buildFavoriteItemDto();
        when(savedCombinationService.addFavorite("user@test.com", 1L)).thenReturn(dto);

        mockMvc.perform(post("/api/favorites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void getUserFavorites_returnsList() throws Exception {
        FavoriteItemDto dto = buildFavoriteItemDto();
        when(savedCombinationService.getUserFavorites("user@test.com")).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/favorites"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    void getFavorites_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/favorites"))
                .andExpect(status().isUnauthorized());
    }
}
