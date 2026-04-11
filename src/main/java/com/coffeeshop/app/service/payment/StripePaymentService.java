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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class StripePaymentService implements PaymentProviderService {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentService.class);
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Value("${app.payment.stripe.webhook-secret:}")
    private String webhookSecret;

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.STRIPE;
    }

    @Override
    public String initiatePayment(Long orderId, BigDecimal amount) {
        // In a real implementation this would call the Stripe API to create a PaymentIntent.
        // For now we generate a stub payment intent ID.
        String externalId = "pi_" + UUID.randomUUID().toString().replace("-", "");
        log.info("Stripe payment initiated for order {} amount {} -> externalId {}", orderId, amount, externalId);
        return externalId;
    }

    @Override
    public boolean isSuccessStatus(String providerStatus) {
        return "succeeded".equalsIgnoreCase(providerStatus)
                || "SUCCESS".equalsIgnoreCase(providerStatus);
    }

    @Override
    public void verifyWebhookSignature(String rawPayload, String signatureHeader) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new IllegalArgumentException("Webhook verification failed");
        }
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new IllegalArgumentException("Webhook verification failed");
        }
        // Stripe signature format: "t=<timestamp>,v1=<hmac>"
        String timestamp = null;
        String expectedSig = null;
        for (String part : signatureHeader.split(",")) {
            if (part.startsWith("t=")) {
                timestamp = part.substring(2);
            } else if (part.startsWith("v1=")) {
                expectedSig = part.substring(3);
            }
        }
        if (timestamp == null || expectedSig == null) {
            throw new IllegalArgumentException("Webhook verification failed");
        }
        try {
            long ts = Long.parseLong(timestamp);
            long now = Instant.now().getEpochSecond();
            if (Math.abs(now - ts) > 300) {
                throw new IllegalArgumentException("Webhook verification failed");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Webhook verification failed");
        }
        String signedPayload = timestamp + "." + rawPayload;
        String computed = hmacSha256(signedPayload, webhookSecret);
        if (!constantTimeEquals(computed, expectedSig)) {
            throw new IllegalArgumentException("Webhook verification failed");
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
        return MessageDigest.isEqual(
                a.getBytes(StandardCharsets.UTF_8),
                b.getBytes(StandardCharsets.UTF_8));
    }
}
