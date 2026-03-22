package com.coffeeshop.app.service;

import com.coffeeshop.app.domain.Role;
import com.coffeeshop.app.domain.User;
import com.coffeeshop.app.dto.auth.AuthResponse;
import com.coffeeshop.app.repository.UserRepository;
import com.coffeeshop.app.security.JwtProperties;
import com.coffeeshop.app.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OtpService otpService;

    private JwtTokenProvider tokenProvider;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("dGVzdC1zZWNyZXQta2V5LWZvci10ZXN0aW5nLW9ubHktbWluaW11bS0yNTYtYml0cy1sb25nLXBhZGRpbmc=");
        props.setAccessTokenExpiration(900000L);
        props.setRefreshTokenExpiration(604800000L);
        tokenProvider = new JwtTokenProvider(props);
        authService = new AuthService(userRepository, otpService, tokenProvider);
    }

    @Test
    void requestCode_newUser_registersAndSendsOtp() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(otpService).generateAndSend("new@example.com");

        authService.requestCode("new@example.com");

        verify(userRepository).saveAndFlush(any(User.class));
        verify(otpService).generateAndSend("new@example.com");
    }

    @Test
    void requestCode_existingUser_onlySendsOtp() {
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);
        doNothing().when(otpService).generateAndSend("existing@example.com");

        authService.requestCode("existing@example.com");

        verify(userRepository, never()).saveAndFlush(any());
        verify(otpService).generateAndSend("existing@example.com");
    }

    @Test
    void verifyCode_validCode_returnsTokens() {
        User user = User.builder().email("user@example.com").role(Role.USER).build();
        when(otpService.verifyCode("user@example.com", "123456")).thenReturn(true);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        AuthResponse response = authService.verifyCode("user@example.com", "123456");

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(response.getEmail()).isEqualTo("user@example.com");
        assertThat(response.getRole()).isEqualTo("USER");
    }

    @Test
    void verifyCode_invalidCode_throwsException() {
        when(otpService.verifyCode(anyString(), anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.verifyCode("user@example.com", "000000"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid or expired OTP code");
    }

    @Test
    void refresh_validToken_returnsNewTokens() {
        User user = User.builder().email("user@example.com").role(Role.USER).build();
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        String refreshToken = tokenProvider.generateRefreshToken("user@example.com", "USER");

        AuthResponse response = authService.refresh(refreshToken);

        assertThat(response.getAccessToken()).isNotBlank();
        assertThat(response.getRefreshToken()).isNotBlank();
        assertThat(response.getEmail()).isEqualTo("user@example.com");
    }

    @Test
    void refresh_invalidToken_throwsException() {
        assertThatThrownBy(() -> authService.refresh("invalid.token.here"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid or expired refresh token");
    }
}
