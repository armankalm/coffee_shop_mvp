package com.coffeeshop.app.service;

import com.coffeeshop.app.domain.*;
import com.coffeeshop.app.dto.payment.PaymentTransactionDto;
import com.coffeeshop.app.repository.OrderRepository;
import com.coffeeshop.app.repository.PaymentTransactionRepository;
import com.coffeeshop.app.service.payment.KaspiPaymentService;
import com.coffeeshop.app.service.payment.StripePaymentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private PaymentTransactionRepository transactionRepository;

    private PaymentService paymentService;

    private User user;
    private CoffeeShop shop;
    private Order order;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("user@test.com").role(Role.USER).build();
        shop = CoffeeShop.builder().id(1L).name("Test Shop").city("Almaty")
                .address("123 St").status(ShopStatus.OPEN).build();
        order = Order.builder()
                .id(1L).user(user).shop(shop)
                .status(OrderStatus.NEW)
                .total(BigDecimal.valueOf(500))
                .build();

        paymentService = new PaymentService(
                orderRepository,
                transactionRepository,
                List.of(new KaspiPaymentService(), new StripePaymentService())
        );
    }

    @Test
    void initiatePayment_kaspi_createsPendingTransaction() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(transactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(inv -> {
                    PaymentTransaction tx = inv.getArgument(0);
                    tx.setId(10L);
                    return tx;
                });

        PaymentTransactionDto result = paymentService.initiatePayment("user@test.com", 1L, PaymentProvider.KASPI);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(result.getProvider()).isEqualTo(PaymentProvider.KASPI);
        assertThat(result.getExternalId()).startsWith("KASPI-");
        verify(transactionRepository).save(any(PaymentTransaction.class));
    }

    @Test
    void initiatePayment_stripe_createsPendingTransaction() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(transactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(inv -> {
                    PaymentTransaction tx = inv.getArgument(0);
                    tx.setId(11L);
                    return tx;
                });

        PaymentTransactionDto result = paymentService.initiatePayment("user@test.com", 1L, PaymentProvider.STRIPE);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(result.getProvider()).isEqualTo(PaymentProvider.STRIPE);
        assertThat(result.getExternalId()).startsWith("pi_");
    }

    @Test
    void initiatePayment_orderNotFound_throwsNoSuchElement() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.initiatePayment("user@test.com", 99L, PaymentProvider.KASPI))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    @Test
    void initiatePayment_wrongUser_throwsIllegalArgument() {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.initiatePayment("other@test.com", 1L, PaymentProvider.KASPI))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void initiatePayment_cancelledOrder_throwsIllegalState() {
        order.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.initiatePayment("user@test.com", 1L, PaymentProvider.KASPI))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void handleWebhook_kaspiSuccess_setsTransactionSuccessAndOrderInProgress() {
        PaymentTransaction tx = PaymentTransaction.builder()
                .id(10L).order(order).provider(PaymentProvider.KASPI)
                .status(PaymentStatus.PENDING).amount(BigDecimal.valueOf(500))
                .externalId("KASPI-abc").build();

        when(transactionRepository.findByExternalId("KASPI-abc")).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentTransactionDto result = paymentService.handleWebhook(PaymentProvider.KASPI, "KASPI-abc", "SUCCESS");

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.IN_PROGRESS);
        verify(orderRepository).save(order);
    }

    @Test
    void handleWebhook_stripeFailed_setsTransactionFailed() {
        PaymentTransaction tx = PaymentTransaction.builder()
                .id(11L).order(order).provider(PaymentProvider.STRIPE)
                .status(PaymentStatus.PENDING).amount(BigDecimal.valueOf(500))
                .externalId("pi_abc").build();

        when(transactionRepository.findByExternalId("pi_abc")).thenReturn(Optional.of(tx));
        when(transactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentTransactionDto result = paymentService.handleWebhook(PaymentProvider.STRIPE, "pi_abc", "failed");

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void handleWebhook_transactionNotFound_throwsNoSuchElement() {
        when(transactionRepository.findByExternalId("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.handleWebhook(PaymentProvider.KASPI, "unknown", "SUCCESS"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void getTransactionsForOrder_returnsAll() {
        PaymentTransaction tx = PaymentTransaction.builder()
                .id(10L).order(order).provider(PaymentProvider.KASPI)
                .status(PaymentStatus.PENDING).amount(BigDecimal.valueOf(500))
                .externalId("KASPI-abc").build();

        when(transactionRepository.findByOrderId(1L)).thenReturn(List.of(tx));

        List<PaymentTransactionDto> result = paymentService.getTransactionsForOrder(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getProvider()).isEqualTo(PaymentProvider.KASPI);
    }
}
