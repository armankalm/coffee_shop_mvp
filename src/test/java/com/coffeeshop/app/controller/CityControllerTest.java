package com.coffeeshop.app.controller;

import com.coffeeshop.app.domain.City;
import com.coffeeshop.app.domain.CoffeeShop;
import com.coffeeshop.app.domain.RefShopStatus;
import com.coffeeshop.app.dto.shop.CityDto;
import com.coffeeshop.app.dto.shop.CoffeeShopDto;
import com.coffeeshop.app.service.CityService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CityController.class)
@Import({com.coffeeshop.app.config.SecurityConfig.class,
         com.coffeeshop.app.config.GlobalExceptionHandler.class,
         com.coffeeshop.app.security.JwtAuthFilter.class,
         com.coffeeshop.app.security.JwtTokenProvider.class,
         com.coffeeshop.app.security.JwtProperties.class})
class CityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CityService cityService;

    private City almaty() {
        return City.builder().id(1L).name("Almaty").region("Almaty").country("KZ").active(true).build();
    }

    @Test
    @WithMockUser
    void getActiveCities_returnsList() throws Exception {
        CityDto dto = CityDto.from(almaty());
        when(cityService.getActiveCities()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/cities"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Almaty"));
    }

    @Test
    @WithMockUser
    void getShopsByCity_returnsShops() throws Exception {
        City city = almaty();
        RefShopStatus status = RefShopStatus.builder().id(1L).code("OPEN").nameRu("Открыто").nameEn("Open").build();
        CoffeeShop shop = CoffeeShop.builder().id(1L).name("Cafe").city(city).address("Addr").status(status).build();
        when(cityService.getShopsByCity(1L)).thenReturn(List.of(CoffeeShopDto.from(shop)));

        mockMvc.perform(get("/api/cities/1/shops"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Cafe"));
    }

    @Test
    @WithMockUser
    void getShopsByCity_notFound_returns404() throws Exception {
        when(cityService.getShopsByCity(99L)).thenThrow(new NoSuchElementException("City not found: 99"));

        mockMvc.perform(get("/api/cities/99/shops"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Resource not found"));
    }

    @Test
    void getActiveCities_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/cities"))
                .andExpect(status().isUnauthorized());
    }
}
