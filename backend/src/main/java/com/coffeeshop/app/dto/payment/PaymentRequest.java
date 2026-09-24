package com.coffeeshop.app.dto.payment;

import jakarta.validation.constraints.NotBlank;

public class PaymentRequest {

    @NotBlank(message = "provider is required")
    private String provider;

    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
}
