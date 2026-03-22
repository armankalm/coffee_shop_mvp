package com.coffeeshop.app.service.payment;

import com.coffeeshop.app.domain.PaymentProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class StripePaymentService implements PaymentProviderService {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentService.class);

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.STRIPE;
    }

    @Override
    public String initiatePayment(Long orderId, BigDecimal amount) {
        // In a real implementation this would call the Stripe API to create a PaymentIntent.
        // For now we generate a stub payment intent ID.
        String externalId = "pi_" + UUID.randomUUID().toString().replace("-", "");
        log.info("Stripe payment initiated for order {} amount {} -> externalId {}", orderId, amount, externalId);
        return externalId;
    }

    @Override
    public boolean isSuccessStatus(String providerStatus) {
        return "succeeded".equalsIgnoreCase(providerStatus)
                || "SUCCESS".equalsIgnoreCase(providerStatus);
    }
}
