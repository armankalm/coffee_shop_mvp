package com.coffeeshop.app.dto.payment;

import com.coffeeshop.app.domain.PaymentProvider;
import com.coffeeshop.app.domain.PaymentStatus;
import com.coffeeshop.app.domain.PaymentTransaction;

import java.math.BigDecimal;
import java.time.Instant;

public class PaymentTransactionDto {

    private Long id;
    private Long orderId;
    private PaymentProvider provider;
    private PaymentStatus status;
    private BigDecimal amount;
    private String externalId;
    private Instant createdAt;
    private Instant updatedAt;

    public static PaymentTransactionDto from(PaymentTransaction tx) {
        PaymentTransactionDto dto = new PaymentTransactionDto();
        dto.id = tx.getId();
        dto.orderId = tx.getOrder().getId();
        dto.provider = tx.getProvider();
        dto.status = tx.getStatus();
        dto.amount = tx.getAmount();
        dto.externalId = tx.getExternalId();
        dto.createdAt = tx.getCreatedAt();
        dto.updatedAt = tx.getUpdatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public PaymentProvider getProvider() { return provider; }
    public PaymentStatus getStatus() { return status; }
    public BigDecimal getAmount() { return amount; }
    public String getExternalId() { return externalId; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
