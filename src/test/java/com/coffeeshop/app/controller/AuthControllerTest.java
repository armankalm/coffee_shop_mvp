package com.coffeeshop.app.controller;

import com.coffeeshop.app.dto.auth.AuthResponse;
import com.coffeeshop.app.service.AuthService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import({com.coffeeshop.app.config.SecurityConfig.class,
         com.coffeeshop.app.config.GlobalExceptionHandler.class,
         com.coffeeshop.app.security.JwtAuthFilter.class,
         com.coffeeshop.app.security.JwtTokenProvider.class,
         com.coffeeshop.app.security.JwtProperties.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AuthService authService;

    @Test
    void requestCode_validEmail_returns200() throws Exception {
        doNothing().when(authService).requestCode("user@example.com");

        mockMvc.perform(post("/api/auth/request-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "user@example.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").exists());

        verify(authService).requestCode("user@example.com");
    }

    @Test
    void requestCode_invalidEmail_returns400() throws Exception {
        mockMvc.perform(post("/api/auth/request-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", "not-an-email"))))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(authService);
    }

    @Test
    void verifyCode_validCredentials_returnsTokens() throws Exception {
        AuthResponse mockResponse = new AuthResponse("access-token", "refresh-token", "user@example.com", "USER");
        when(authService.verifyCode("user@example.com", "123456")).thenReturn(mockResponse);

        mockMvc.perform(post("/api/auth/verify-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "user@example.com", "code", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("access-token"))
                .andExpect(jsonPath("$.refreshToken").value("refresh-token"))
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.role").value("USER"));
    }

    @Test
    void verifyCode_invalidOtp_returns400() throws Exception {
        when(authService.verifyCode(anyString(), anyString()))
                .thenThrow(new IllegalArgumentException("Invalid or expired OTP code"));

        mockMvc.perform(post("/api/auth/verify-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("email", "user@example.com", "code", "000000"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Invalid or expired OTP code"));
    }

    @Test
    void refresh_validToken_returnsNewTokens() throws Exception {
        AuthResponse mockResponse = new AuthResponse("new-access", "new-refresh", "user@example.com", "USER");
        when(authService.refresh("old-refresh-token")).thenReturn(mockResponse);

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", "old-refresh-token"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("new-access"));
    }

    @Test
    void refresh_invalidToken_returns400() throws Exception {
        when(authService.refresh(anyString()))
                .thenThrow(new IllegalArgumentException("Invalid or expired refresh token"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("refreshToken", "bad-token"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void protectedEndpoint_withoutToken_returns401() throws Exception {
        mockMvc.perform(post("/api/orders"))
                .andExpect(status().isUnauthorized());
    }
}
