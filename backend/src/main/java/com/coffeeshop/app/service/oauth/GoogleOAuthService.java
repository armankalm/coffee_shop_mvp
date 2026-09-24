package com.coffeeshop.app.service.oauth;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Google OAuth 2.0 authorization-code flow, done by hand to keep the API stateless:
 * the backend redirects to Google, Google redirects back to {@link #redirectUri()},
 * and the code is exchanged here for the user's verified email.
 */
@Service
public class GoogleOAuthService {

    private static final String AUTHORIZE_URL = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_URL = "https://oauth2.googleapis.com/token";
    private static final String USERINFO_URL = "https://openidconnect.googleapis.com/v1/userinfo";

    public static final String CALLBACK_PATH = "/api/auth/oauth/google/callback";

    private final RestClient restClient;
    private final String clientId;
    private final String clientSecret;
    private final String backendUrl;

    public GoogleOAuthService(RestClient.Builder restClientBuilder,
                              @Value("${app.oauth.google.client-id:}") String clientId,
                              @Value("${app.oauth.google.client-secret:}") String clientSecret,
                              @Value("${app.backend-url:http://localhost:8080}") String backendUrl) {
        this.restClient = restClientBuilder.build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.backendUrl = backendUrl.replaceAll("/+$", "");
    }

    public boolean isConfigured() {
        return !clientId.isBlank() && !clientSecret.isBlank();
    }

    /** Must be registered in Google Cloud Console as an authorized redirect URI. */
    public String redirectUri() {
        return backendUrl + CALLBACK_PATH;
    }

    public String authorizationUrl(String state) {
        return UriComponentsBuilder.fromHttpUrl(AUTHORIZE_URL)
                .queryParam("client_id", clientId)
                .queryParam("redirect_uri", redirectUri())
                .queryParam("response_type", "code")
                .queryParam("scope", "openid email profile")
                .queryParam("state", state)
                .queryParam("prompt", "select_account")
                .encode()
                .toUriString();
    }

    public GoogleUser fetchUser(String code) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("code", code);
        form.add("client_id", clientId);
        form.add("client_secret", clientSecret);
        form.add("redirect_uri", redirectUri());
        form.add("grant_type", "authorization_code");

        try {
            JsonNode token = restClient.post()
                    .uri(TOKEN_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(JsonNode.class);
            String accessToken = token == null ? null : token.path("access_token").asText(null);
            if (accessToken == null) {
                throw new OAuthException("provider_failed");
            }

            JsonNode info = restClient.get()
                    .uri(USERINFO_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .body(JsonNode.class);
            String email = info == null ? null : info.path("email").asText(null);
            if (email == null || !info.path("email_verified").asBoolean(false)) {
                throw new OAuthException("provider_email_missing");
            }
            return new GoogleUser(email, info.path("name").asText(null));
        } catch (RestClientException e) {
            throw new OAuthException("provider_failed", e);
        }
    }

    public record GoogleUser(String email, String name) {
    }
}
