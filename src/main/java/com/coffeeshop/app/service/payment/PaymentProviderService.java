package com.coffeeshop.app.service.payment;

import com.coffeeshop.app.domain.PaymentProvider;

import java.math.BigDecimal;

public interface PaymentProviderService {

    PaymentProvider getProvider();

    /**
     * Initiates a payment and returns an external transaction ID.
     */
    String initiatePayment(Long orderId, BigDecimal amount);

    /**
     * Maps a provider-specific status string to SUCCESS or FAILED.
     */
    boolean isSuccessStatus(String providerStatus);

    /**
     * Verifies that the webhook signature header is valid for the given raw payload.
     * Throws IllegalArgumentException if signature is invalid or missing.
     */
    void verifyWebhookSignature(String rawPayload, String signatureHeader);
}
