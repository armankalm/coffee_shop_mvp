package com.coffeeshop.app.dto.payment;

import jakarta.validation.constraints.NotNull;

public class PaymentRequest {

    @NotNull(message = "provider is required")
    private String provider;

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
}
