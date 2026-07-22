package com.coffeeshop.app.service.payment;

/**
 * Result of initiating a payment with a provider.
 *
 * @param externalId          provider transaction/intent id (persisted, matched by webhooks)
 * @param clientSecret        secret the client uses to confirm the payment (Stripe), or null
 * @param providerCustomerId  provider customer id to persist for reusing saved cards, or null
 */
public record PaymentInitiation(
        String externalId,
        String clientSecret,
        String providerCustomerId) {
}
