package com.coffeeshop.app.service;

import com.coffeeshop.app.config.AccessDeniedException;
import com.coffeeshop.app.domain.*;
import com.coffeeshop.app.dto.payment.PaymentTransactionDto;
import com.coffeeshop.app.repository.OrderRepository;
import com.coffeeshop.app.repository.PaymentTransactionRepository;
import com.coffeeshop.app.repository.RefOrderStatusRepository;
import com.coffeeshop.app.service.payment.PaymentProviderService;
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
    private final Map<PaymentProvider, PaymentProviderService> providers;
    private final RefOrderStatusRepository refOrderStatusRepository;

    public PaymentService(OrderRepository orderRepository,
                          PaymentTransactionRepository transactionRepository,
                          List<PaymentProviderService> providerList,
                          RefOrderStatusRepository refOrderStatusRepository) {
        this.orderRepository = orderRepository;
        this.transactionRepository = transactionRepository;
        this.providers = providerList.stream()
                .collect(Collectors.toMap(PaymentProviderService::getProvider, Function.identity()));
        this.refOrderStatusRepository = refOrderStatusRepository;
    }

    public PaymentTransactionDto initiatePayment(String userEmail, Long orderId, PaymentProvider provider) {
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

        String externalId = providerService.initiatePayment(orderId, order.getTotal());

        PaymentTransaction tx = PaymentTransaction.builder()
                .order(order)
                .provider(provider)
                .status(PaymentStatus.PENDING)
                .amount(order.getTotal())
                .externalId(externalId)
                .build();

        try {
            return PaymentTransactionDto.from(transactionRepository.save(tx));
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

        if (success && "NEW".equals(order.getStatus().getCode())) {
            RefOrderStatus inProgressStatus = refOrderStatusRepository.findByCode("IN_PROGRESS")
                    .orElseThrow(() -> new NoSuchElementException("Order status IN_PROGRESS not found in reference table"));
            order.setStatus(inProgressStatus);
            orderRepository.save(order);
        }

        return PaymentTransactionDto.from(transactionRepository.save(tx));
    }

    @Transactional(readOnly = true)
    public List<PaymentTransactionDto> getTransactionsForOrder(Long orderId) {
        return transactionRepository.findByOrderId(orderId).stream()
                .map(PaymentTransactionDto::from)
                .collect(Collectors.toList());
    }
}
