package com.coffeeshop.app.dto.payment;

import jakarta.validation.constraints.NotBlank;

public class PaymentRequest {

    @NotBlank(message = "provider is required")
    private String provider;

    /**
     * Optional id of a previously saved payment method (card) to charge.
     * When null, the client collects a new card via the provider's payment element.
     */
    private String paymentMethodId;

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getPaymentMethodId() { return paymentMethodId; }
    public void setPaymentMethodId(String paymentMethodId) { this.paymentMethodId = paymentMethodId; }
}
