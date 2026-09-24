package com.coffeeshop.app.service.push;

import com.coffeeshop.app.domain.PushSubscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.time.Duration;
import java.time.Instant;

/**
 * Delivers one encrypted message to one browser push endpoint, authenticated with VAPID
 * (RFC 8292). Without VAPID keys configured, push is disabled and nothing is sent.
 */
@Service
public class WebPushSender {

    public enum Result { SENT, GONE, FAILED }

    private static final Logger log = LoggerFactory.getLogger(WebPushSender.class);
    private static final Duration VAPID_TOKEN_TTL = Duration.ofHours(12);

    private final String publicKey;
    private final ECPrivateKey privateKey;
    private final String subject;
    private final HttpClient httpClient;

    public WebPushSender(@Value("${app.push.vapid-public-key:}") String publicKey,
                         @Value("${app.push.vapid-private-key:}") String privateKey,
                         @Value("${app.push.vapid-subject:}") String subject) {
        this.publicKey = publicKey.trim();
        this.subject = subject.isBlank() ? "mailto:admin@example.com" : subject.trim();
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
        this.privateKey = parseKeys(this.publicKey, privateKey.trim());
        if (this.privateKey == null) {
            log.warn("Web Push is disabled: VAPID_PUBLIC_KEY / VAPID_PRIVATE_KEY are not set");
        }
    }

    private static ECPrivateKey parseKeys(String publicKey, String privateKey) {
        if (publicKey.isEmpty() || privateKey.isEmpty()) {
            return null;
        }
        try {
            WebPushCrypto.decodePublicKey(WebPushCrypto.base64UrlDecode(publicKey));
            return WebPushCrypto.decodePrivateKey(WebPushCrypto.base64UrlDecode(privateKey));
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalStateException("VAPID_PUBLIC_KEY / VAPID_PRIVATE_KEY are not valid P-256 keys", e);
        }
    }

    public boolean isEnabled() {
        return privateKey != null;
    }

    /** Base64url application server key the browser subscribes with. */
    public String getPublicKey() {
        return publicKey;
    }

    /**
     * @param ttlSeconds how long the push service keeps the message for an offline browser
     */
    public Result send(PushSubscription subscription, byte[] payload, int ttlSeconds) {
        if (!isEnabled()) {
            return Result.FAILED;
        }
        try {
            byte[] body = WebPushCrypto.encrypt(payload,
                    WebPushCrypto.base64UrlDecode(subscription.getP256dh()),
                    WebPushCrypto.base64UrlDecode(subscription.getAuth()));
            URI endpoint = URI.create(subscription.getEndpoint());

            HttpRequest request = HttpRequest.newBuilder(endpoint)
                    .timeout(Duration.ofSeconds(15))
                    .header("TTL", String.valueOf(ttlSeconds))
                    .header("Urgency", "high")
                    .header("Content-Encoding", "aes128gcm")
                    .header("Content-Type", "application/octet-stream")
                    .header("Authorization", "vapid t=" + vapidToken(endpoint) + ", k=" + publicKey)
                    .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            if (status >= 200 && status < 300) {
                return Result.SENT;
            }
            if (status == 404 || status == 410) {
                return Result.GONE; // the browser unsubscribed or the subscription expired
            }
            log.warn("Push service {} responded {}: {}", endpoint.getHost(), status, response.body());
            return Result.FAILED;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return Result.FAILED;
        } catch (Exception e) {
            log.warn("Failed to send push notification: {}", e.getMessage());
            return Result.FAILED;
        }
    }

    /** ES256 JWT whose audience is the push service origin (RFC 8292 section 2). */
    String vapidToken(URI endpoint) throws GeneralSecurityException {
        String audience = endpoint.getScheme() + "://" + endpoint.getRawAuthority();
        long expiresAt = Instant.now().plus(VAPID_TOKEN_TTL).getEpochSecond();
        String header = encodeJson("{\"typ\":\"JWT\",\"alg\":\"ES256\"}");
        String claims = encodeJson("{\"aud\":\"" + audience + "\",\"exp\":" + expiresAt
                + ",\"sub\":\"" + subject.replace("\"", "") + "\"}");
        String signingInput = header + "." + claims;

        // P1363 format gives the raw R||S signature JWS expects, rather than DER.
        Signature signer = Signature.getInstance("SHA256withECDSAinP1363Format");
        signer.initSign(privateKey);
        signer.update(signingInput.getBytes(StandardCharsets.US_ASCII));
        return signingInput + "." + WebPushCrypto.base64UrlEncode(signer.sign());
    }

    private static String encodeJson(String json) {
        return WebPushCrypto.base64UrlEncode(json.getBytes(StandardCharsets.UTF_8));
    }
}
