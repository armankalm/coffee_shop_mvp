package com.coffeeshop.app.service.payment;

import java.math.BigDecimal;

/**
 * Input for initiating a payment with a provider.
 *
 * @param orderId              the order being paid
 * @param amount               order total in major currency units (e.g. tenge)
 * @param customerEmail        the paying user's email (used to create/find a provider customer)
 * @param providerCustomerId   existing provider customer id, or null to create one
 * @param savedPaymentMethodId a previously saved payment method to charge, or null to let the
 *                             customer enter a new card on the client
 */
public record PaymentContext(
        Long orderId,
        BigDecimal amount,
        String customerEmail,
        String providerCustomerId,
        String savedPaymentMethodId) {
}
