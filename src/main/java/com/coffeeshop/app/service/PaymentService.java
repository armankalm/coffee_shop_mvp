package com.coffeeshop.app.service;

import com.coffeeshop.app.config.AccessDeniedException;
import com.coffeeshop.app.domain.*;
import com.coffeeshop.app.dto.payment.PaymentTransactionDto;
import com.coffeeshop.app.dto.payment.SavedCardDto;
import com.coffeeshop.app.repository.OrderRepository;
import com.coffeeshop.app.repository.PaymentTransactionRepository;
import com.coffeeshop.app.repository.RefOrderStatusRepository;
import com.coffeeshop.app.repository.UserRepository;
import com.coffeeshop.app.service.board.OrderStatusChangedEvent;
import com.coffeeshop.app.service.payment.PaymentContext;
import com.coffeeshop.app.service.payment.PaymentInitiation;
import com.coffeeshop.app.service.payment.PaymentProviderService;
import com.coffeeshop.app.service.print.NewOrderEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional
public class PaymentService {

    private final OrderRepository orderRepository;
    private final PaymentTransactionRepository transactionRepository;
    private final UserRepository userRepository;
    private final Map<PaymentProvider, PaymentProviderService> providers;
    private final RefOrderStatusRepository refOrderStatusRepository;
    private final ApplicationEventPublisher eventPublisher;

    public PaymentService(OrderRepository orderRepository,
                          PaymentTransactionRepository transactionRepository,
                          UserRepository userRepository,
                          List<PaymentProviderService> providerList,
                          RefOrderStatusRepository refOrderStatusRepository,
                          ApplicationEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.transactionRepository = transactionRepository;
        this.userRepository = userRepository;
        this.providers = providerList.stream()
                .collect(Collectors.toMap(PaymentProviderService::getProvider, Function.identity()));
        this.refOrderStatusRepository = refOrderStatusRepository;
        this.eventPublisher = eventPublisher;
    }

    public PaymentTransactionDto initiatePayment(String userEmail, Long orderId, PaymentProvider provider,
                                                 String savedPaymentMethodId) {
        Order order = orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));

        if (!order.getUser().getEmail().equals(userEmail)) {
            throw new AccessDeniedException("Access denied to order: " + orderId);
        }

        String orderStatusCode = order.getStatus().getCode();
        if ("CANCELLED".equals(orderStatusCode) || "COMPLETED".equals(orderStatusCode)) {
            throw new IllegalStateException("Cannot pay for order in status: " + orderStatusCode);
        }

        transactionRepository.findByOrderIdAndStatus(orderId, PaymentStatus.PENDING)
                .ifPresent(existing -> {
                    throw new IllegalStateException("A pending payment already exists for order: " + orderId);
                });
        transactionRepository.findByOrderIdAndStatus(orderId, PaymentStatus.SUCCESS)
                .ifPresent(existing -> {
                    throw new IllegalStateException("Order has already been paid: " + orderId);
                });

        PaymentProviderService providerService = providers.get(provider);
        if (providerService == null) {
            throw new IllegalArgumentException("Unsupported payment provider: " + provider);
        }

        User user = order.getUser();
        PaymentContext context = new PaymentContext(
                orderId,
                order.getTotal(),
                user.getEmail(),
                user.getStripeCustomerId(),
                savedPaymentMethodId);

        PaymentInitiation initiation = providerService.initiatePayment(context);

        // Persist a newly created provider customer id so saved cards can be reused next time.
        if (initiation.providerCustomerId() != null
                && !initiation.providerCustomerId().equals(user.getStripeCustomerId())) {
            user.setStripeCustomerId(initiation.providerCustomerId());
            userRepository.save(user);
        }

        PaymentTransaction tx = PaymentTransaction.builder()
                .order(order)
                .provider(provider)
                .status(PaymentStatus.PENDING)
                .amount(order.getTotal())
                .externalId(initiation.externalId())
                .build();

        try {
            PaymentTransaction saved = transactionRepository.save(tx);
            return PaymentTransactionDto.from(saved, initiation.clientSecret());
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("A pending payment already exists for order: " + orderId);
        }
    }

    public void verifyWebhook(PaymentProvider provider, String rawPayload, String signatureHeader) {
        PaymentProviderService providerService = providers.get(provider);
        if (providerService == null) {
            throw new IllegalArgumentException("Unsupported payment provider: " + provider);
        }
        providerService.verifyWebhookSignature(rawPayload, signatureHeader);
    }

    public PaymentTransactionDto handleWebhook(PaymentProvider provider, String externalId, String providerStatus) {
        PaymentTransaction tx = transactionRepository.findByExternalId(externalId)
                .orElseThrow(() -> new NoSuchElementException("Transaction not found: " + externalId));

        if (tx.getProvider() != provider) {
            throw new IllegalArgumentException("Provider mismatch for transaction: " + externalId);
        }

        PaymentProviderService providerService = providers.get(provider);
        if (providerService == null) {
            throw new IllegalArgumentException("Unsupported payment provider: " + provider);
        }

        if (tx.getStatus() != PaymentStatus.PENDING) {
            return PaymentTransactionDto.from(tx);
        }

        boolean success = providerService.isSuccessStatus(providerStatus);
        tx.setStatus(success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);

        Order order = orderRepository.findByIdWithDetails(tx.getOrder().getId())
                .orElseThrow(() -> new NoSuchElementException("Order not found for transaction: " + externalId));
        tx.setOrder(order);

        // Promote the unpaid draft into the active NEW queue once payment succeeds.
        boolean promoted = false;
        if (success && "PENDING_PAYMENT".equals(order.getStatus().getCode())) {
            RefOrderStatus newStatus = refOrderStatusRepository.findByCode("NEW")
                    .orElseThrow(() -> new NoSuchElementException("Order status NEW not found in reference table"));
            order.setStatus(newStatus);
            orderRepository.save(order);
            promoted = true;
        }

        PaymentTransaction savedTx = transactionRepository.save(tx);

        // Fire kitchen/print/board notifications only once the order becomes real (paid).
        if (promoted) {
            eventPublisher.publishEvent(new NewOrderEvent(this, order.getId()));
            eventPublisher.publishEvent(new OrderStatusChangedEvent(this, order.getShop().getId(), order.getId()));
        }

        return PaymentTransactionDto.from(savedTx);
    }

    @Transactional(readOnly = true)
    public List<PaymentTransactionDto> getTransactionsForOrder(Long orderId) {
        return transactionRepository.findByOrderId(orderId).stream()
                .map(PaymentTransactionDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SavedCardDto> getSavedCards(String userEmail, PaymentProvider provider) {
        PaymentProviderService providerService = providers.get(provider);
        if (providerService == null) {
            throw new IllegalArgumentException("Unsupported payment provider: " + provider);
        }
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userEmail));
        return providerService.listSavedCards(user.getStripeCustomerId());
    }

    public void deleteSavedCard(String userEmail, PaymentProvider provider, String paymentMethodId) {
        PaymentProviderService providerService = providers.get(provider);
        if (providerService == null) {
            throw new IllegalArgumentException("Unsupported payment provider: " + provider);
        }
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userEmail));
        if (user.getStripeCustomerId() == null) {
            throw new NoSuchElementException("No saved cards for user");
        }
        providerService.deleteSavedCard(user.getStripeCustomerId(), paymentMethodId);
    }
}
