package com.coffeeshop.app.service.payment;

/**
 * Minimal view of a verified Stripe webhook event relevant to payments.
 *
 * @param externalId the PaymentIntent id (matches the persisted transaction), or null if not payment-related
 * @param status     the PaymentIntent status (e.g. "succeeded"), or null
 * @param relevant   whether this event should update a payment transaction
 */
public record StripeWebhookEvent(String externalId, String status, boolean relevant) {
}
