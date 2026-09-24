package com.coffeeshop.app.dto.payment;

import jakarta.validation.constraints.NotBlank;

public class WebhookPayload {

    @NotBlank(message = "transactionId is required")
    private String transactionId;

    @NotBlank(message = "status is required")
    private String status;

    public String getTransactionId() { return transactionId; }
    public void setTransactionId(String transactionId) { this.transactionId = transactionId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
