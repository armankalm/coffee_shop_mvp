package com.coffeeshop.app.controller;

import com.coffeeshop.app.dto.auth.AuthResponse;
import com.coffeeshop.app.service.AuthService;
import com.coffeeshop.app.service.oauth.GoogleOAuthService;
import com.coffeeshop.app.service.oauth.OAuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Browser-facing Google sign-in. The flow ends with a redirect to
 * {@code <frontend>/auth/callback#accessToken=...&refreshToken=...} (or {@code #error=<code>}).
 * Tokens go in the URL fragment so they never reach any server logs.
 */
@RestController
@RequestMapping("/api/auth/oauth/google")
public class OAuthController {

    private static final Logger log = LoggerFactory.getLogger(OAuthController.class);
    private static final String STATE_COOKIE = "oauth_state";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final GoogleOAuthService googleOAuthService;
    private final AuthService authService;
    private final String frontendUrl;
    private final boolean secureCookie;

    public OAuthController(GoogleOAuthService googleOAuthService,
                           AuthService authService,
                           @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl,
                           @Value("${app.backend-url:http://localhost:8080}") String backendUrl) {
        this.googleOAuthService = googleOAuthService;
        this.authService = authService;
        this.frontendUrl = frontendUrl.replaceAll("/+$", "");
        this.secureCookie = backendUrl.startsWith("https://");
    }

    @GetMapping
    public ResponseEntity<Void> start() {
        if (!googleOAuthService.isConfigured()) {
            return redirectToFrontend(Map.of("error", "not_configured"), null);
        }

        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        String state = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

        // CSRF protection: the state must come back from Google and match this cookie.
        ResponseCookie cookie = stateCookie(state, Duration.ofMinutes(10));
        return ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, googleOAuthService.authorizationUrl(state))
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> callback(@RequestParam(required = false) String code,
                                         @RequestParam(required = false) String state,
                                         @RequestParam(required = false) String error,
                                         @CookieValue(name = STATE_COOKIE, required = false) String expectedState) {
        ResponseCookie clearCookie = stateCookie("", Duration.ZERO);

        if (error != null) {
            return redirectToFrontend(Map.of("error", "access_denied"), clearCookie);
        }
        if (state == null || expectedState == null
                || !MessageDigest.isEqual(state.getBytes(StandardCharsets.UTF_8), expectedState.getBytes(StandardCharsets.UTF_8))) {
            return redirectToFrontend(Map.of("error", "invalid_state"), clearCookie);
        }
        if (code == null || code.isBlank()) {
            return redirectToFrontend(Map.of("error", "missing_code"), clearCookie);
        }

        try {
            GoogleOAuthService.GoogleUser googleUser = googleOAuthService.fetchUser(code);
            AuthResponse auth = authService.loginWithGoogle(googleUser.email(), googleUser.name());

            Map<String, String> params = new LinkedHashMap<>();
            params.put("accessToken", auth.getAccessToken());
            params.put("refreshToken", auth.getRefreshToken());
            params.put("email", auth.getEmail());
            params.put("role", auth.getRole());
            return redirectToFrontend(params, clearCookie);
        } catch (OAuthException e) {
            log.warn("Google sign-in failed: {}", e.getErrorCode(), e.getCause());
            return redirectToFrontend(Map.of("error", e.getErrorCode()), clearCookie);
        }
    }

    private ResponseCookie stateCookie(String value, Duration maxAge) {
        return ResponseCookie.from(STATE_COOKIE, value)
                .httpOnly(true)
                .secure(secureCookie)
                .sameSite("Lax")
                .path("/api/auth/oauth")
                .maxAge(maxAge)
                .build();
    }

    private ResponseEntity<Void> redirectToFrontend(Map<String, String> params, ResponseCookie cookie) {
        String fragment = params.entrySet().stream()
                .map(e -> e.getKey() + "=" + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
        ResponseEntity.BodyBuilder response = ResponseEntity.status(HttpStatus.FOUND)
                .header(HttpHeaders.LOCATION, frontendUrl + "/auth/callback#" + fragment);
        if (cookie != null) {
            response.header(HttpHeaders.SET_COOKIE, cookie.toString());
        }
        return response.build();
    }
}
