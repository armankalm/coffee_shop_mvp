package com.coffeeshop.app.service.payment;

import com.coffeeshop.app.domain.PaymentProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class KaspiPaymentService implements PaymentProviderService {

    private static final Logger log = LoggerFactory.getLogger(KaspiPaymentService.class);

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.KASPI;
    }

    @Override
    public String initiatePayment(Long orderId, BigDecimal amount) {
        // In a real implementation this would call the Kaspi Pay API.
        // For now we generate a stub transaction ID.
        String externalId = "KASPI-" + UUID.randomUUID();
        log.info("Kaspi payment initiated for order {} amount {} -> externalId {}", orderId, amount, externalId);
        return externalId;
    }

    @Override
    public boolean isSuccessStatus(String providerStatus) {
        return "SUCCESS".equalsIgnoreCase(providerStatus)
                || "PAID".equalsIgnoreCase(providerStatus);
    }
}
