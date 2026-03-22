package com.coffeeshop.app.service.payment;

import com.coffeeshop.app.domain.PaymentProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class KaspiPaymentService implements PaymentProviderService {

    private static final Logger log = LoggerFactory.getLogger(KaspiPaymentService.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Value("${app.payment.kaspi.api-key:}")
    private String apiKey;

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.KASPI;
    }

    @Override
    public String initiatePayment(Long orderId, BigDecimal amount) {
        // In a real implementation this would call the Kaspi Pay API.
        // For now we generate a stub transaction ID.
        String externalId = "KASPI-" + UUID.randomUUID();
        log.info("Kaspi payment initiated for order {} amount {} -> externalId {}", orderId, amount, externalId);
        return externalId;
    }

    @Override
    public boolean isSuccessStatus(String providerStatus) {
        return "SUCCESS".equalsIgnoreCase(providerStatus)
                || "PAID".equalsIgnoreCase(providerStatus);
    }

    @Override
    public void verifyWebhookSignature(String rawPayload, String signatureHeader) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("Kaspi API key is not configured");
        }
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new IllegalArgumentException("Missing X-Kaspi-Signature header");
        }
        String computed = hmacSha256(rawPayload, apiKey);
        if (!constantTimeEquals(computed, signatureHeader)) {
            throw new IllegalArgumentException("Kaspi webhook signature verification failed");
        }
    }

    private String hmacSha256(String data, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("Failed to compute HMAC-SHA256", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length()) return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
