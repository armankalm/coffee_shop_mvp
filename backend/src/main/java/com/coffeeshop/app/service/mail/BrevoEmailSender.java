package com.coffeeshop.app.service.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.Map;

/**
 * Sends transactional email through the Brevo HTTP API.
 * SMTP is not an option on Render's free plan (outbound ports 25/465/587 are blocked),
 * so mail goes over HTTPS instead. The sender address must be a verified sender in Brevo.
 */
@Service
public class BrevoEmailSender {

    private static final Logger log = LoggerFactory.getLogger(BrevoEmailSender.class);
    private static final String API_URL = "https://api.brevo.com/v3/smtp/email";

    private final RestClient restClient;
    private final String apiKey;
    private final String fromAddress;
    private final String fromName;

    public BrevoEmailSender(RestClient.Builder restClientBuilder,
                            @Value("${app.mail.brevo-api-key:}") String apiKey,
                            @Value("${app.mail.from-address:}") String fromAddress,
                            @Value("${app.mail.from-name:Coffee Shop}") String fromName) {
        this.restClient = restClientBuilder.baseUrl(API_URL).build();
        this.apiKey = extractApiKey(apiKey);
        this.fromAddress = fromAddress;
        this.fromName = fromName;
        if (isConfigured() && fromAddress.isBlank()) {
            throw new IllegalStateException("MAIL_FROM_ADDRESS must be set when BREVO_API_KEY is configured");
        }
    }

    /**
     * Accepts either the bare key (xkeysib-...) or a Symfony Mailer DSN such as
     * brevo+api://xkeysib-...@default, so the same value can be shared between projects.
     */
    static String extractApiKey(String value) {
        String key = value == null ? "" : value.trim();
        int scheme = key.indexOf("://");
        if (scheme >= 0) {
            key = key.substring(scheme + 3);
            int at = key.lastIndexOf('@');
            if (at >= 0) {
                key = key.substring(0, at);
            }
        }
        return key;
    }

    public boolean isConfigured() {
        return !apiKey.isBlank();
    }

    public void send(String to, String subject, String text) {
        Map<String, Object> body = Map.of(
                "sender", Map.of("email", fromAddress, "name", fromName),
                "to", List.of(Map.of("email", to)),
                "subject", subject,
                "textContent", text
        );
        try {
            restClient.post()
                    .header("api-key", apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientResponseException e) {
            // Brevo explains 401s in the body (bad key vs. unauthorized IP); keep it in the log.
            throw new IllegalStateException("Brevo responded " + e.getStatusCode().value()
                    + ": " + e.getResponseBodyAsString(), e);
        }
        log.debug("Brevo accepted email to {}", to);
    }
}
