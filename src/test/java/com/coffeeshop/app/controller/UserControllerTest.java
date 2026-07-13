package com.coffeeshop.app.controller;

import com.coffeeshop.app.domain.*;
import com.coffeeshop.app.dto.admin.UserDto;
import com.coffeeshop.app.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import com.coffeeshop.app.dto.user.UpdateShopRequest;
import java.util.Map;
import java.util.NoSuchElementException;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UserController.class)
@Import({com.coffeeshop.app.config.SecurityConfig.class,
         com.coffeeshop.app.config.GlobalExceptionHandler.class,
         com.coffeeshop.app.security.JwtAuthFilter.class,
         com.coffeeshop.app.security.JwtTokenProvider.class,
         com.coffeeshop.app.security.JwtProperties.class})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserService userService;

    private UserDto buildUserDto(Long shopId) {
        RefUserRole userRole = RefUserRole.builder().id(1L).code("USER").nameRu("Пользователь").nameEn("User").build();
        City city = City.builder().id(1L).name("Almaty").active(true).build();
        RefShopStatus status = RefShopStatus.builder().id(1L).code("OPEN").nameRu("Открыта").nameEn("Open").build();
        CoffeeShop shop = CoffeeShop.builder().id(shopId).name("Test Shop").city(city).address("123 St").status(status).build();
        User user = User.builder().id(1L).email("user@test.com").role(userRole).coffeeShop(shop).build();
        return UserDto.fromWithDetails(user);
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = "USER")
    void getCurrentUser_authenticated_returns200() throws Exception {
        UserDto mockDto = buildUserDto(5L);
        when(userService.getCurrentUser("user@test.com")).thenReturn(mockDto);

        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("user@test.com"))
                .andExpect(jsonPath("$.coffeeShopId").value(5))
                .andExpect(jsonPath("$.coffeeShopName").value("Test Shop"))
                .andExpect(jsonPath("$.coffeeShop.id").value(5))
                .andExpect(jsonPath("$.coffeeShop.name").value("Test Shop"))
                .andExpect(jsonPath("$.coffeeShop.address").value("123 St"))
                .andExpect(jsonPath("$.coffeeShop.status").value("OPEN"));

        verify(userService).getCurrentUser("user@test.com");
    }

    @Test
    void getCurrentUser_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = "USER")
    void updateMyShop_validRequest_returns200() throws Exception {
        UserDto mockDto = buildUserDto(2L);
        when(userService.updateUserShop("user@test.com", 2L)).thenReturn(mockDto);

        mockMvc.perform(patch("/api/users/me/shop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("shopId", 2))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coffeeShopId").value(2));

        verify(userService).updateUserShop("user@test.com", 2L);
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = "USER")
    void updateMyShop_shopNotFound_returns404() throws Exception {
        when(userService.updateUserShop("user@test.com", 99L))
                .thenThrow(new NoSuchElementException("Coffee shop not found: 99"));

        mockMvc.perform(patch("/api/users/me/shop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("shopId", 99))))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateMyShop_unauthenticated_returns401() throws Exception {
        mockMvc.perform(patch("/api/users/me/shop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("shopId", 1))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "barista@test.com", roles = "BARISTA")
    void updateMyShop_baristaRole_returns200() throws Exception {
        UserDto mockDto = buildUserDto(2L);
        when(userService.updateUserShop("barista@test.com", 2L)).thenReturn(mockDto);

        mockMvc.perform(patch("/api/users/me/shop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("shopId", 2))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void updateMyShop_adminRole_returns200() throws Exception {
        UserDto mockDto = buildUserDto(1L);
        when(userService.updateUserShop("admin@test.com", 1L)).thenReturn(mockDto);

        mockMvc.perform(patch("/api/users/me/shop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("shopId", 1))))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = "USER")
    void updateMyShop_missingShopId_returns400() throws Exception {
        mockMvc.perform(patch("/api/users/me/shop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = "USER")
    void updateMyShop_negativeShopId_returns400() throws Exception {
        mockMvc.perform(patch("/api/users/me/shop")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("shopId", -1))))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = "USER")
    void updateMyProfile_validRequest_returns200() throws Exception {
        UserDto mockDto = buildUserDto(5L);
        when(userService.updateProfile("user@test.com", "Алия Садыкова", "+7 701 555 24 10")).thenReturn(mockDto);

        mockMvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("name", "Алия Садыкова", "phone", "+7 701 555 24 10"))))
                .andExpect(status().isOk());

        verify(userService).updateProfile("user@test.com", "Алия Садыкова", "+7 701 555 24 10");
    }

    @Test
    void updateMyProfile_unauthenticated_returns401() throws Exception {
        mockMvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", "Test"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = "USER")
    void updateMyProfile_nameTooLong_returns400() throws Exception {
        String tooLong = "a".repeat(256);

        mockMvc.perform(patch("/api/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("name", tooLong))))
                .andExpect(status().isBadRequest());
    }
}
