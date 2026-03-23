package com.coffeeshop.app.controller;

import com.coffeeshop.app.service.DemoDataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DemoController.class)
@ActiveProfiles("demo")
@Import({com.coffeeshop.app.config.SecurityConfig.class,
         com.coffeeshop.app.config.GlobalExceptionHandler.class,
         com.coffeeshop.app.security.JwtAuthFilter.class,
         com.coffeeshop.app.security.JwtTokenProvider.class,
         com.coffeeshop.app.security.JwtProperties.class})
class DemoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DemoDataService demoDataService;

    @Test
    @WithMockUser(roles = "ADMIN")
    void stats_returnsCountMap() throws Exception {
        when(demoDataService.stats()).thenReturn(Map.of(
                "cities", 5L,
                "shops", 10L,
                "products", 50L,
                "toppings", 30L,
                "users", 5L,
                "orders", 20L,
                "savedCombinations", 10L
        ));

        mockMvc.perform(get("/api/admin/demo/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cities").value(5))
                .andExpect(jsonPath("$.shops").value(10))
                .andExpect(jsonPath("$.users").value(5));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void reset_callsServiceAndReturnsStats() throws Exception {
        when(demoDataService.stats()).thenReturn(Map.of(
                "cities", 5L,
                "shops", 10L,
                "products", 50L,
                "toppings", 30L,
                "users", 5L,
                "orders", 20L,
                "savedCombinations", 10L
        ));

        mockMvc.perform(post("/api/admin/demo/reset"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Demo data reset successfully"))
                .andExpect(jsonPath("$.stats.cities").value(5));

        verify(demoDataService).reset();
    }

    @Test
    @WithMockUser(roles = "BARISTA")
    void stats_baristaForbidden_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/demo/stats"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "BARISTA")
    void reset_baristaForbidden_returns403() throws Exception {
        mockMvc.perform(post("/api/admin/demo/reset"))
                .andExpect(status().isForbidden());
    }

    @Test
    void stats_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/demo/stats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void reset_unauthenticated_returns401() throws Exception {
        mockMvc.perform(post("/api/admin/demo/reset"))
                .andExpect(status().isUnauthorized());
    }
}
