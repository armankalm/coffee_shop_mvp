package com.coffeeshop.app.controller;

import com.coffeeshop.app.dto.auth.AuthResponse;
import com.coffeeshop.app.service.AuthService;
import com.coffeeshop.app.service.oauth.GoogleOAuthService;
import com.coffeeshop.app.service.oauth.OAuthException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OAuthControllerTest {

    @Mock
    private GoogleOAuthService googleOAuthService;

    @Mock
    private AuthService authService;

    private OAuthController controller;

    @BeforeEach
    void setUp() {
        controller = new OAuthController(googleOAuthService, authService,
                "https://front.example.com/", "https://api.example.com");
    }

    @Test
    void start_notConfigured_redirectsToFrontendWithError() {
        when(googleOAuthService.isConfigured()).thenReturn(false);

        ResponseEntity<Void> response = controller.start();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(location(response)).isEqualTo("https://front.example.com/auth/callback#error=not_configured");
    }

    @Test
    void start_redirectsToGoogleAndSetsSecureStateCookie() {
        when(googleOAuthService.isConfigured()).thenReturn(true);
        when(googleOAuthService.authorizationUrl(anyString())).thenReturn("https://accounts.google.com/auth");

        ResponseEntity<Void> response = controller.start();

        assertThat(location(response)).isEqualTo("https://accounts.google.com/auth");
        String cookie = response.getHeaders().getFirst(HttpHeaders.SET_COOKIE);
        assertThat(cookie).startsWith("oauth_state=").contains("HttpOnly", "Secure", "SameSite=Lax");
    }

    @Test
    void callback_stateMismatch_rejectsWithoutCallingGoogle() {
        ResponseEntity<Void> response = controller.callback("code", "state-a", null, "state-b");

        assertThat(location(response)).endsWith("#error=invalid_state");
        verifyNoInteractions(authService);
        verify(googleOAuthService, never()).fetchUser(anyString());
    }

    @Test
    void callback_missingCookie_rejects() {
        ResponseEntity<Void> response = controller.callback("code", "state", null, null);

        assertThat(location(response)).endsWith("#error=invalid_state");
    }

    @Test
    void callback_userCancelled_redirectsWithAccessDenied() {
        ResponseEntity<Void> response = controller.callback(null, "state", "access_denied", "state");

        assertThat(location(response)).endsWith("#error=access_denied");
    }

    @Test
    void callback_success_redirectsWithTokensInFragment() {
        when(googleOAuthService.fetchUser("code"))
                .thenReturn(new GoogleOAuthService.GoogleUser("User@Gmail.com", "User"));
        when(authService.loginWithGoogle("User@Gmail.com", "User"))
                .thenReturn(new AuthResponse("access", "refresh", "user@gmail.com", "USER"));

        ResponseEntity<Void> response = controller.callback("code", "state", null, "state");

        assertThat(location(response)).isEqualTo(
                "https://front.example.com/auth/callback#accessToken=access&refreshToken=refresh&email=user%40gmail.com&role=USER");
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");
    }

    @Test
    void callback_unverifiedEmail_redirectsWithProviderError() {
        when(googleOAuthService.fetchUser("code")).thenThrow(new OAuthException("provider_email_missing"));

        ResponseEntity<Void> response = controller.callback("code", "state", null, "state");

        assertThat(location(response)).endsWith("#error=provider_email_missing");
        verifyNoInteractions(authService);
    }

    private static String location(ResponseEntity<Void> response) {
        return response.getHeaders().getFirst(HttpHeaders.LOCATION);
    }
}
