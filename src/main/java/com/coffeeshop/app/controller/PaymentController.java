package com.coffeeshop.app.controller;

import com.coffeeshop.app.domain.PaymentProvider;
import com.coffeeshop.app.dto.payment.PaymentRequest;
import com.coffeeshop.app.dto.payment.PaymentTransactionDto;
import com.coffeeshop.app.dto.payment.SavedCardDto;
import com.coffeeshop.app.dto.payment.WebhookPayload;
import com.coffeeshop.app.service.PaymentService;
import com.coffeeshop.app.service.payment.StripePaymentService;
import com.coffeeshop.app.service.payment.StripeWebhookEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
public class PaymentController {

    private final PaymentService paymentService;
    private final StripePaymentService stripePaymentService;
    private final ObjectMapper objectMapper;

    public PaymentController(PaymentService paymentService,
                            StripePaymentService stripePaymentService,
                            ObjectMapper objectMapper) {
        this.paymentService = paymentService;
        this.stripePaymentService = stripePaymentService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/api/orders/{id}/pay")
    public ResponseEntity<PaymentTransactionDto> pay(
            Authentication authentication,
            @PathVariable("id") Long orderId,
            @Valid @RequestBody PaymentRequest request) {
        PaymentProvider provider = parseProvider(request.getProvider());
        PaymentTransactionDto tx = paymentService.initiatePayment(
                authentication.getName(), orderId, provider, request.getPaymentMethodId());
        return ResponseEntity.ok(tx);
    }

    @GetMapping("/api/payment-methods")
    public ResponseEntity<List<SavedCardDto>> savedCards(Authentication authentication) {
        return ResponseEntity.ok(
                paymentService.getSavedCards(authentication.getName(), PaymentProvider.STRIPE));
    }

    @DeleteMapping("/api/payment-methods/{paymentMethodId}")
    public ResponseEntity<Void> deleteSavedCard(
            Authentication authentication,
            @PathVariable("paymentMethodId") String paymentMethodId) {
        paymentService.deleteSavedCard(authentication.getName(), PaymentProvider.STRIPE, paymentMethodId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/api/payments/webhook/kaspi")
    public ResponseEntity<Void> kaspiWebhook(
            HttpServletRequest httpRequest,
            @RequestBody byte[] rawBody) throws IOException {
        String signature = httpRequest.getHeader("X-Kaspi-Signature");
        String rawPayload = new String(rawBody, StandardCharsets.UTF_8);
        paymentService.verifyWebhook(PaymentProvider.KASPI, rawPayload, signature);
        WebhookPayload payload = objectMapper.readValue(rawBody, WebhookPayload.class);
        if (payload.getTransactionId() == null || payload.getTransactionId().isBlank()) {
            throw new IllegalArgumentException("transactionId is required");
        }
        if (payload.getStatus() == null || payload.getStatus().isBlank()) {
            throw new IllegalArgumentException("status is required");
        }
        paymentService.handleWebhook(PaymentProvider.KASPI, payload.getTransactionId(), payload.getStatus());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/api/payments/webhook/stripe")
    public ResponseEntity<Void> stripeWebhook(
            HttpServletRequest httpRequest,
            @RequestBody byte[] rawBody) {
        String signature = httpRequest.getHeader("Stripe-Signature");
        String rawPayload = new String(rawBody, StandardCharsets.UTF_8);
        // constructEvent both verifies the signature and parses the Stripe event.
        StripeWebhookEvent event = stripePaymentService.parseWebhookEvent(rawPayload, signature);
        if (event.relevant() && event.externalId() != null) {
            paymentService.handleWebhook(PaymentProvider.STRIPE, event.externalId(), event.status());
        }
        return ResponseEntity.ok().build();
    }

    private PaymentProvider parseProvider(String raw) {
        try {
            return PaymentProvider.valueOf(raw.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unsupported payment provider: " + raw);
        }
    }
}
