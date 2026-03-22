package com.coffeeshop.app.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class JwtTokenProviderTest {

    private JwtTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        JwtProperties props = new JwtProperties();
        props.setSecret("dGVzdC1zZWNyZXQta2V5LWZvci10ZXN0aW5nLW9ubHktbWluaW11bS0yNTYtYml0cy1sb25nLXBhZGRpbmc=");
        props.setAccessTokenExpiration(900000L);
        props.setRefreshTokenExpiration(604800000L);
        tokenProvider = new JwtTokenProvider(props);
    }

    @Test
    void generateAndValidateAccessToken() {
        String token = tokenProvider.generateAccessToken("user@example.com", "USER");

        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getEmailFromToken(token)).isEqualTo("user@example.com");
        assertThat(tokenProvider.getRoleFromToken(token)).isEqualTo("USER");
    }

    @Test
    void generateAndValidateRefreshToken() {
        String token = tokenProvider.generateRefreshToken("admin@example.com", "ADMIN");

        assertThat(tokenProvider.validateToken(token)).isTrue();
        assertThat(tokenProvider.getEmailFromToken(token)).isEqualTo("admin@example.com");
        assertThat(tokenProvider.getRoleFromToken(token)).isEqualTo("ADMIN");
    }

    @Test
    void validateToken_invalidToken_returnsFalse() {
        assertThat(tokenProvider.validateToken("not.a.valid.token")).isFalse();
    }

    @Test
    void validateToken_tamperedToken_returnsFalse() {
        String token = tokenProvider.generateAccessToken("user@example.com", "USER");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";
        assertThat(tokenProvider.validateToken(tampered)).isFalse();
    }

    @Test
    void expiredToken_returnsFalse() {
        JwtProperties shortProps = new JwtProperties();
        shortProps.setSecret("dGVzdC1zZWNyZXQta2V5LWZvci10ZXN0aW5nLW9ubHktbWluaW11bS0yNTYtYml0cy1sb25nLXBhZGRpbmc=");
        shortProps.setAccessTokenExpiration(-1000L); // already expired
        shortProps.setRefreshTokenExpiration(604800000L);
        JwtTokenProvider shortProvider = new JwtTokenProvider(shortProps);

        String token = shortProvider.generateAccessToken("user@example.com", "USER");
        assertThat(shortProvider.validateToken(token)).isFalse();
    }
}
