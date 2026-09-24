package com.coffeeshop.app.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The production jar bundles the SPA in classpath:/static; test resources stand in for it. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SpaFallbackTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rootForwardsToIndexWithoutAuth() throws Exception {
        // Spring Boot's welcome page handler forwards "/" to index.html (MockMvc doesn't follow forwards).
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(forwardedUrl("index.html"));
    }

    @Test
    void indexHtmlIsServed() throws Exception {
        mockMvc.perform(get("/index.html"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("spa-index")));
    }

    @Test
    void clientSideRouteFallsBackToIndex() throws Exception {
        mockMvc.perform(get("/orders/in-progress"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("spa-index")));
    }

    @Test
    void authCallbackRouteIsServedToBrowser() throws Exception {
        mockMvc.perform(get("/auth/callback"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("spa-index")));
    }

    @Test
    void existingAssetIsServedAsIs() throws Exception {
        mockMvc.perform(get("/assets/app.js"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("asset")));
    }

    @Test
    void apiStillRequiresAuth() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }
}
