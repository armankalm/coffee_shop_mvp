package com.coffeeshop.app.controller;

import com.coffeeshop.app.domain.PaymentProvider;
import com.coffeeshop.app.dto.payment.PaymentRequest;
import com.coffeeshop.app.dto.payment.PaymentTransactionDto;
import com.coffeeshop.app.dto.payment.WebhookPayload;
import com.coffeeshop.app.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/api/orders/{id}/pay")
    public ResponseEntity<PaymentTransactionDto> pay(
            Authentication authentication,
            @PathVariable("id") Long orderId,
            @Valid @RequestBody PaymentRequest request) {
        PaymentProvider provider = PaymentProvider.valueOf(request.getProvider().toUpperCase());
        PaymentTransactionDto tx = paymentService.initiatePayment(
                authentication.getName(), orderId, provider);
        return ResponseEntity.ok(tx);
    }

    @PostMapping("/api/payments/webhook/kaspi")
    public ResponseEntity<PaymentTransactionDto> kaspiWebhook(@RequestBody WebhookPayload payload) {
        PaymentTransactionDto tx = paymentService.handleWebhook(
                PaymentProvider.KASPI, payload.getTransactionId(), payload.getStatus());
        return ResponseEntity.ok(tx);
    }

    @PostMapping("/api/payments/webhook/stripe")
    public ResponseEntity<PaymentTransactionDto> stripeWebhook(@RequestBody WebhookPayload payload) {
        PaymentTransactionDto tx = paymentService.handleWebhook(
                PaymentProvider.STRIPE, payload.getTransactionId(), payload.getStatus());
        return ResponseEntity.ok(tx);
    }
}
