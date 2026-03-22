package com.coffeeshop.app.service;

import com.coffeeshop.app.domain.*;
import com.coffeeshop.app.dto.payment.PaymentTransactionDto;
import com.coffeeshop.app.repository.OrderRepository;
import com.coffeeshop.app.repository.PaymentTransactionRepository;
import com.coffeeshop.app.service.payment.PaymentProviderService;
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

    public PaymentService(OrderRepository orderRepository,
                          PaymentTransactionRepository transactionRepository,
                          List<PaymentProviderService> providerList) {
        this.orderRepository = orderRepository;
        this.transactionRepository = transactionRepository;
        this.providers = providerList.stream()
                .collect(Collectors.toMap(PaymentProviderService::getProvider, Function.identity()));
    }

    public PaymentTransactionDto initiatePayment(String userEmail, Long orderId, PaymentProvider provider) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NoSuchElementException("Order not found: " + orderId));

        if (!order.getUser().getEmail().equals(userEmail)) {
            throw new IllegalArgumentException("Access denied to order: " + orderId);
        }

        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.COMPLETED) {
            throw new IllegalStateException("Cannot pay for order in status: " + order.getStatus());
        }

        transactionRepository.findByOrderIdAndStatus(orderId, PaymentStatus.PENDING)
                .ifPresent(existing -> {
                    throw new IllegalStateException("A pending payment already exists for order: " + orderId);
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

        return PaymentTransactionDto.from(transactionRepository.save(tx));
    }

    public PaymentTransactionDto handleWebhook(PaymentProvider provider, String externalId, String providerStatus) {
        PaymentTransaction tx = transactionRepository.findByExternalId(externalId)
                .orElseThrow(() -> new NoSuchElementException("Transaction not found: " + externalId));

        PaymentProviderService providerService = providers.get(provider);
        if (providerService == null) {
            throw new IllegalArgumentException("Unsupported payment provider: " + provider);
        }

        boolean success = providerService.isSuccessStatus(providerStatus);
        tx.setStatus(success ? PaymentStatus.SUCCESS : PaymentStatus.FAILED);

        if (success) {
            tx.getOrder().setStatus(OrderStatus.IN_PROGRESS);
            orderRepository.save(tx.getOrder());
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
