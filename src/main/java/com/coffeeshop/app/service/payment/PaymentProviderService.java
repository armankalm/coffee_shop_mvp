package com.coffeeshop.app.service.payment;

import com.coffeeshop.app.domain.PaymentProvider;
import com.coffeeshop.app.dto.payment.SavedCardDto;

import java.util.List;

public interface PaymentProviderService {

    PaymentProvider getProvider();

    /**
     * Initiates a payment and returns the provider transaction id plus, where applicable,
     * a client secret used to confirm the payment on the client and the provider customer id.
     */
    PaymentInitiation initiatePayment(PaymentContext context);

    /**
     * Maps a provider-specific status string to SUCCESS or FAILED.
     */
    boolean isSuccessStatus(String providerStatus);

    /**
     * Verifies that the webhook signature header is valid for the given raw payload.
     * Throws IllegalArgumentException if signature is invalid or missing.
     */
    void verifyWebhookSignature(String rawPayload, String signatureHeader);

    /**
     * Lists the customer's saved cards. Providers without saved-card support return an empty list.
     */
    default List<SavedCardDto> listSavedCards(String providerCustomerId) {
        return List.of();
    }

    /**
     * Detaches (removes) a saved card from the customer. No-op for providers without support.
     */
    default void deleteSavedCard(String providerCustomerId, String paymentMethodId) {
    }
}
