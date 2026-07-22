package com.coffeeshop.app.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

/**
 * Initializes the global Stripe API key from configuration.
 * The secret key is never exposed to clients — only the backend uses it.
 */
@Configuration
public class StripeConfig {

    private static final Logger log = LoggerFactory.getLogger(StripeConfig.class);

    @Value("${app.payment.stripe.secret-key:}")
    private String secretKey;

    @PostConstruct
    public void init() {
        if (secretKey == null || secretKey.isBlank()) {
            log.warn("Stripe secret key is not configured; Stripe payments will fail until STRIPE_SECRET_KEY is set.");
            return;
        }
        Stripe.apiKey = secretKey;
        log.info("Stripe API key configured.");
    }
}
