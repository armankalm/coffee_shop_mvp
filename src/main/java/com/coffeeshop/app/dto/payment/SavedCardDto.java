package com.coffeeshop.app.dto.payment;

/**
 * A saved card (Stripe payment method) shown to the user for reuse.
 * Contains only non-sensitive display data — never the full card number.
 */
public record SavedCardDto(
        String id,
        String brand,
        String last4,
        Long expMonth,
        Long expYear) {
}
