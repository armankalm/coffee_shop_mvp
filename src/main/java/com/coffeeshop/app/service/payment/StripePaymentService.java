package com.coffeeshop.app.service.payment;

import com.coffeeshop.app.domain.PaymentProvider;
import com.coffeeshop.app.dto.payment.SavedCardDto;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentMethodListParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class StripePaymentService implements PaymentProviderService {

    private static final Logger log = LoggerFactory.getLogger(StripePaymentService.class);

    /**
     * Multiplier converting major currency units to Stripe's minor units.
     * KZT is a 2-decimal currency, so amounts are sent as total * 100.
     */
    private static final BigDecimal MINOR_UNIT_FACTOR = BigDecimal.valueOf(100);

    @Value("${app.payment.stripe.webhook-secret:}")
    private String webhookSecret;

    @Value("${app.payment.currency:kzt}")
    private String currency;

    @Override
    public PaymentProvider getProvider() {
        return PaymentProvider.STRIPE;
    }

    @Override
    public PaymentInitiation initiatePayment(PaymentContext context) {
        try {
            String customerId = resolveCustomerId(context);

            long amountMinor = context.amount()
                    .multiply(MINOR_UNIT_FACTOR)
                    .setScale(0, java.math.RoundingMode.HALF_UP)
                    .longValueExact();

            PaymentIntentCreateParams.Builder params = PaymentIntentCreateParams.builder()
                    .setAmount(amountMinor)
                    .setCurrency(currency)
                    .setCustomer(customerId)
                    // Save the card to the customer so it can be reused for future orders.
                    .setSetupFutureUsage(PaymentIntentCreateParams.SetupFutureUsage.OFF_SESSION)
                    .putMetadata("orderId", String.valueOf(context.orderId()));

            String savedPaymentMethodId = context.savedPaymentMethodId();
            if (savedPaymentMethodId != null && !savedPaymentMethodId.isBlank()) {
                // Charge an existing saved card and confirm immediately (off-session style, but
                // the user is present so no separate authentication step is forced here).
                params.setPaymentMethod(savedPaymentMethodId)
                        .setConfirm(true)
                        .setOffSession(true);
            } else {
                // Let the client collect a new card via the Payment Element.
                params.setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build());
            }

            PaymentIntent intent = PaymentIntent.create(params.build());
            log.info("Stripe PaymentIntent {} created for order {} amount {} {}",
                    intent.getId(), context.orderId(), amountMinor, currency);
            return new PaymentInitiation(intent.getId(), intent.getClientSecret(), customerId);
        } catch (StripeException e) {
            log.error("Stripe payment initiation failed for order {}: {}", context.orderId(), e.getMessage());
            throw new IllegalStateException("Failed to initiate Stripe payment: " + e.getMessage(), e);
        }
    }

    private String resolveCustomerId(PaymentContext context) throws StripeException {
        if (context.providerCustomerId() != null && !context.providerCustomerId().isBlank()) {
            return context.providerCustomerId();
        }
        CustomerCreateParams params = CustomerCreateParams.builder()
                .setEmail(context.customerEmail())
                .build();
        Customer customer = Customer.create(params);
        log.info("Created Stripe customer {} for {}", customer.getId(), context.customerEmail());
        return customer.getId();
    }

    @Override
    public boolean isSuccessStatus(String providerStatus) {
        return "succeeded".equalsIgnoreCase(providerStatus)
                || "SUCCESS".equalsIgnoreCase(providerStatus);
    }

    @Override
    public void verifyWebhookSignature(String rawPayload, String signatureHeader) {
        constructVerifiedEvent(rawPayload, signatureHeader);
    }

    /**
     * Verifies the signature and parses the Stripe event, extracting the PaymentIntent id and
     * status for the payment lifecycle events we care about.
     */
    public StripeWebhookEvent parseWebhookEvent(String rawPayload, String signatureHeader) {
        Event event = constructVerifiedEvent(rawPayload, signatureHeader);

        String type = event.getType();
        boolean relevant = "payment_intent.succeeded".equals(type)
                || "payment_intent.payment_failed".equals(type);
        if (!relevant) {
            return new StripeWebhookEvent(null, null, false);
        }

        StripeObject stripeObject = event.getDataObjectDeserializer().getObject().orElse(null);
        if (!(stripeObject instanceof PaymentIntent intent)) {
            log.warn("Stripe event {} of type {} did not deserialize to a PaymentIntent", event.getId(), type);
            return new StripeWebhookEvent(null, null, false);
        }
        return new StripeWebhookEvent(intent.getId(), intent.getStatus(), true);
    }

    private Event constructVerifiedEvent(String rawPayload, String signatureHeader) {
        if (webhookSecret == null || webhookSecret.isBlank()) {
            throw new IllegalArgumentException("Webhook verification failed");
        }
        if (signatureHeader == null || signatureHeader.isBlank()) {
            throw new IllegalArgumentException("Webhook verification failed");
        }
        try {
            return Webhook.constructEvent(rawPayload, signatureHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            throw new IllegalArgumentException("Webhook verification failed");
        }
    }

    @Override
    public List<SavedCardDto> listSavedCards(String providerCustomerId) {
        if (providerCustomerId == null || providerCustomerId.isBlank()) {
            return List.of();
        }
        try {
            PaymentMethodListParams params = PaymentMethodListParams.builder()
                    .setCustomer(providerCustomerId)
                    .setType(PaymentMethodListParams.Type.CARD)
                    .build();
            List<SavedCardDto> cards = new ArrayList<>();
            for (PaymentMethod pm : PaymentMethod.list(params).getData()) {
                PaymentMethod.Card card = pm.getCard();
                if (card == null) {
                    continue;
                }
                cards.add(new SavedCardDto(
                        pm.getId(),
                        card.getBrand(),
                        card.getLast4(),
                        card.getExpMonth(),
                        card.getExpYear()));
            }
            return cards;
        } catch (StripeException e) {
            log.error("Failed to list Stripe saved cards for customer {}: {}", providerCustomerId, e.getMessage());
            throw new IllegalStateException("Failed to list saved cards: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteSavedCard(String providerCustomerId, String paymentMethodId) {
        try {
            PaymentMethod pm = PaymentMethod.retrieve(paymentMethodId);
            // Ensure the card actually belongs to this customer before detaching.
            if (pm.getCustomer() == null || !pm.getCustomer().equals(providerCustomerId)) {
                throw new IllegalArgumentException("Payment method does not belong to the customer");
            }
            pm.detach();
            log.info("Detached Stripe payment method {} from customer {}", paymentMethodId, providerCustomerId);
        } catch (StripeException e) {
            log.error("Failed to detach Stripe payment method {}: {}", paymentMethodId, e.getMessage());
            throw new IllegalStateException("Failed to delete saved card: " + e.getMessage(), e);
        }
    }
}
