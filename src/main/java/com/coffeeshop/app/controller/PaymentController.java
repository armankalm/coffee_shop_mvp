package com.coffeeshop.app.controller;

import com.coffeeshop.app.domain.PaymentProvider;
import com.coffeeshop.app.dto.payment.PaymentRequest;
import com.coffeeshop.app.dto.payment.PaymentTransactionDto;
import com.coffeeshop.app.dto.payment.WebhookPayload;
import com.coffeeshop.app.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@RestController
public class PaymentController {

    private final PaymentService paymentService;
    private final ObjectMapper objectMapper;

    public PaymentController(PaymentService paymentService, ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/api/orders/{id}/pay")
    public ResponseEntity<PaymentTransactionDto> pay(
            Authentication authentication,
            @PathVariable("id") Long orderId,
            @Valid @RequestBody PaymentRequest request) {
        PaymentProvider provider;
        try {
            provider = PaymentProvider.valueOf(request.getProvider().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported payment provider: " + request.getProvider());
        }
        PaymentTransactionDto tx = paymentService.initiatePayment(
                authentication.getName(), orderId, provider);
        return ResponseEntity.ok(tx);
    }

    @PostMapping("/api/payments/webhook/kaspi")
    public ResponseEntity<PaymentTransactionDto> kaspiWebhook(
            HttpServletRequest httpRequest,
            @RequestBody byte[] rawBody) throws IOException {
        String signature = httpRequest.getHeader("X-Kaspi-Signature");
        String rawPayload = new String(rawBody, StandardCharsets.UTF_8);
        paymentService.verifyWebhook(PaymentProvider.KASPI, rawPayload, signature);
        WebhookPayload payload = objectMapper.readValue(rawBody, WebhookPayload.class);
        PaymentTransactionDto tx = paymentService.handleWebhook(
                PaymentProvider.KASPI, payload.getTransactionId(), payload.getStatus());
        return ResponseEntity.ok(tx);
    }

    @PostMapping("/api/payments/webhook/stripe")
    public ResponseEntity<PaymentTransactionDto> stripeWebhook(
            HttpServletRequest httpRequest,
            @RequestBody byte[] rawBody) throws IOException {
        String signature = httpRequest.getHeader("Stripe-Signature");
        String rawPayload = new String(rawBody, StandardCharsets.UTF_8);
        paymentService.verifyWebhook(PaymentProvider.STRIPE, rawPayload, signature);
        WebhookPayload payload = objectMapper.readValue(rawBody, WebhookPayload.class);
        PaymentTransactionDto tx = paymentService.handleWebhook(
                PaymentProvider.STRIPE, payload.getTransactionId(), payload.getStatus());
        return ResponseEntity.ok(tx);
    }
}
