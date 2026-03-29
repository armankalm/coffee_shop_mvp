package com.coffeeshop.app.service;

import com.coffeeshop.app.domain.CoffeeShop;
import com.coffeeshop.app.domain.RefShopStatus;
import com.coffeeshop.app.domain.RefUserRole;
import com.coffeeshop.app.domain.User;
import com.coffeeshop.app.dto.auth.AuthResponse;
import com.coffeeshop.app.repository.CoffeeShopRepository;
import com.coffeeshop.app.repository.RefUserRoleRepository;
import com.coffeeshop.app.repository.UserRepository;
import com.coffeeshop.app.security.JwtProperties;
import com.coffeeshop.app.security.JwtTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OtpService otpService;

    @Mock
    private RefUserRoleRepository refUserRoleRepository;

    @Mock
    private CoffeeShopRepository coffeeShopRepository;

    private JwtTokenProvider tokenProvider;

    private AuthService authService;

    private RefUserRole userRole;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("dGVzdC1zZWNyZXQta2V5LWZvci10ZXN0aW5nLW9ubHktbWluaW11bS0yNTYtYml0cy1sb25nLXBhZGRpbmc=");
        props.setAccessTokenExpiration(900000L);
        props.setRefreshTokenExpiration(604800000L);
        tokenProvider = new JwtTokenProvider(props);
        authService = new AuthService(userRepository, otpService, tokenProvider, refUserRoleRepository, coffeeShopRepository);

        userRole = RefUserRole.builder().id(1L).code("USER").nameRu("Пользователь").nameEn("User").build();
    }

    @Test
    void requestCode_newUser_registersAndSendsOtp() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(refUserRoleRepository.findByCode("USER")).thenReturn(Optional.of(userRole));
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
        User user = User.builder().email("user@example.com").role(userRole).build();
        when(otpService.verifyCode("user@example.com", "123456")).thenReturn(true);
        when(userRepository.findByEmailWithRole("user@example.com")).thenReturn(Optional.of(user));

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
        User user = User.builder().email("user@example.com").role(userRole).build();
        when(userRepository.findByEmailWithRole("user@example.com")).thenReturn(Optional.of(user));
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

    @Test
    void requestCode_newUser_assignsDefaultActiveCoffeeShop() {
        RefShopStatus activeStatus = RefShopStatus.builder().id(1L).code("ACTIVE").nameRu("Активна").nameEn("Active").build();
        CoffeeShop shop = CoffeeShop.builder().id(1L).name("Test Shop").address("Test Address").status(activeStatus).build();

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(refUserRoleRepository.findByCode("USER")).thenReturn(Optional.of(userRole));
        when(coffeeShopRepository.findFirstByStatusCode(eq("ACTIVE"), any(Pageable.class))).thenReturn(List.of(shop));
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(otpService).generateAndSend("new@example.com");

        authService.requestCode("new@example.com");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getCoffeeShop()).isEqualTo(shop);
    }

    @Test
    void requestCode_newUser_noActiveShop_assignsNullCoffeeShop() {
        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(refUserRoleRepository.findByCode("USER")).thenReturn(Optional.of(userRole));
        when(coffeeShopRepository.findFirstByStatusCode(eq("ACTIVE"), any(Pageable.class))).thenReturn(List.of());
        when(userRepository.saveAndFlush(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        doNothing().when(otpService).generateAndSend("new@example.com");

        authService.requestCode("new@example.com");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getCoffeeShop()).isNull();
    }
}
