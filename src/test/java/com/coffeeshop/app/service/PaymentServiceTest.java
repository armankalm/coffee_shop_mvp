package com.coffeeshop.app.service;

import com.coffeeshop.app.config.AccessDeniedException;
import com.coffeeshop.app.domain.*;
import com.coffeeshop.app.dto.payment.PaymentTransactionDto;
import com.coffeeshop.app.repository.OrderRepository;
import com.coffeeshop.app.repository.PaymentTransactionRepository;
import com.coffeeshop.app.repository.RefOrderStatusRepository;
import com.coffeeshop.app.repository.UserRepository;
import com.coffeeshop.app.service.payment.PaymentContext;
import com.coffeeshop.app.service.payment.PaymentInitiation;
import com.coffeeshop.app.service.payment.PaymentProviderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

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
    @Mock private UserRepository userRepository;
    @Mock private RefOrderStatusRepository refOrderStatusRepository;
    @Mock private ApplicationEventPublisher eventPublisher;

    private PaymentProviderService kaspiProvider;
    private PaymentProviderService stripeProvider;

    private PaymentService paymentService;

    private RefUserRole userRole;
    private RefShopStatus openStatus;
    private RefOrderStatus newStatus;
    private RefOrderStatus pendingPaymentStatus;
    private RefOrderStatus cancelledStatus;

    private User user;
    private CoffeeShop shop;
    private Order order;

    @BeforeEach
    void setUp() {
        userRole = RefUserRole.builder().id(1L).code("USER").nameRu("Пользователь").nameEn("User").build();
        openStatus = RefShopStatus.builder().id(1L).code("OPEN").nameRu("Открыто").nameEn("Open").build();
        newStatus = RefOrderStatus.builder().id(1L).code("NEW").nameRu("Новый").nameEn("New").build();
        pendingPaymentStatus = RefOrderStatus.builder().id(6L).code("PENDING_PAYMENT")
                .nameRu("Ожидает оплаты").nameEn("Awaiting Payment").build();
        cancelledStatus = RefOrderStatus.builder().id(5L).code("CANCELLED").nameRu("Отменён").nameEn("Cancelled").build();

        user = User.builder().id(1L).email("user@test.com").role(userRole).build();
        City almatyCity = City.builder().id(1L).name("Almaty").active(true).build();
        shop = CoffeeShop.builder().id(1L).name("Test Shop").city(almatyCity)
                .address("123 St").status(openStatus).build();
        order = Order.builder()
                .id(1L).user(user).shop(shop)
                .status(pendingPaymentStatus)
                .total(BigDecimal.valueOf(500))
                .build();

        // Stubbed providers so tests never hit the real Stripe/Kaspi APIs.
        kaspiProvider = mock(PaymentProviderService.class);
        stripeProvider = mock(PaymentProviderService.class);
        when(kaspiProvider.getProvider()).thenReturn(PaymentProvider.KASPI);
        when(stripeProvider.getProvider()).thenReturn(PaymentProvider.STRIPE);

        paymentService = new PaymentService(
                orderRepository,
                transactionRepository,
                userRepository,
                List.of(kaspiProvider, stripeProvider),
                refOrderStatusRepository,
                eventPublisher
        );
    }

    @Test
    void initiatePayment_kaspi_createsPendingTransaction() {
        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));
        when(kaspiProvider.initiatePayment(any(PaymentContext.class)))
                .thenReturn(new PaymentInitiation("KASPI-abc", null, null));
        when(transactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(inv -> {
                    PaymentTransaction tx = inv.getArgument(0);
                    tx.setId(10L);
                    return tx;
                });

        PaymentTransactionDto result = paymentService.initiatePayment(
                "user@test.com", 1L, PaymentProvider.KASPI, null);

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(result.getProvider()).isEqualTo(PaymentProvider.KASPI);
        assertThat(result.getExternalId()).isEqualTo("KASPI-abc");
        verify(transactionRepository).save(any(PaymentTransaction.class));
    }

    @Test
    void initiatePayment_stripe_returnsClientSecretAndPersistsCustomerId() {
        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));
        when(stripeProvider.initiatePayment(any(PaymentContext.class)))
                .thenReturn(new PaymentInitiation("pi_abc", "pi_abc_secret_123", "cus_new"));
        when(transactionRepository.save(any(PaymentTransaction.class)))
                .thenAnswer(inv -> {
                    PaymentTransaction tx = inv.getArgument(0);
                    tx.setId(11L);
                    return tx;
                });

        PaymentTransactionDto result = paymentService.initiatePayment(
                "user@test.com", 1L, PaymentProvider.STRIPE, null);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(result.getProvider()).isEqualTo(PaymentProvider.STRIPE);
        assertThat(result.getExternalId()).isEqualTo("pi_abc");
        assertThat(result.getClientSecret()).isEqualTo("pi_abc_secret_123");
        // A newly created Stripe customer id must be persisted on the user for reuse.
        assertThat(user.getStripeCustomerId()).isEqualTo("cus_new");
        verify(userRepository).save(user);
    }

    @Test
    void initiatePayment_stripe_withSavedCard_passesPaymentMethodId() {
        user.setStripeCustomerId("cus_existing");
        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));
        when(stripeProvider.initiatePayment(any(PaymentContext.class)))
                .thenReturn(new PaymentInitiation("pi_saved", "secret", "cus_existing"));
        when(transactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        paymentService.initiatePayment("user@test.com", 1L, PaymentProvider.STRIPE, "pm_card_123");

        ArgumentCaptor<PaymentContext> ctx = ArgumentCaptor.forClass(PaymentContext.class);
        verify(stripeProvider).initiatePayment(ctx.capture());
        assertThat(ctx.getValue().savedPaymentMethodId()).isEqualTo("pm_card_123");
        assertThat(ctx.getValue().providerCustomerId()).isEqualTo("cus_existing");
        // Existing customer id unchanged, so no redundant user save.
        verify(userRepository, never()).save(any());
    }

    @Test
    void initiatePayment_orderNotFound_throwsNoSuchElement() {
        when(orderRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> paymentService.initiatePayment("user@test.com", 99L, PaymentProvider.KASPI, null))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    @Test
    void initiatePayment_wrongUser_throwsAccessDenied() {
        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.initiatePayment("other@test.com", 1L, PaymentProvider.KASPI, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void initiatePayment_cancelledOrder_throwsIllegalState() {
        order.setStatus(cancelledStatus);
        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> paymentService.initiatePayment("user@test.com", 1L, PaymentProvider.KASPI, null))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void initiatePayment_pendingTransactionExists_throwsIllegalState() {
        PaymentTransaction pendingTx = PaymentTransaction.builder()
                .id(5L).order(order).provider(PaymentProvider.KASPI)
                .status(PaymentStatus.PENDING).amount(BigDecimal.valueOf(500))
                .externalId("KASPI-pending").build();

        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));
        when(transactionRepository.findByOrderIdAndStatus(1L, PaymentStatus.PENDING))
                .thenReturn(Optional.of(pendingTx));

        assertThatThrownBy(() -> paymentService.initiatePayment("user@test.com", 1L, PaymentProvider.KASPI, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("pending payment already exists");
    }

    @Test
    void initiatePayment_successTransactionExists_throwsIllegalState() {
        PaymentTransaction successTx = PaymentTransaction.builder()
                .id(6L).order(order).provider(PaymentProvider.KASPI)
                .status(PaymentStatus.SUCCESS).amount(BigDecimal.valueOf(500))
                .externalId("KASPI-paid").build();

        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));
        when(transactionRepository.findByOrderIdAndStatus(1L, PaymentStatus.PENDING))
                .thenReturn(Optional.empty());
        when(transactionRepository.findByOrderIdAndStatus(1L, PaymentStatus.SUCCESS))
                .thenReturn(Optional.of(successTx));

        assertThatThrownBy(() -> paymentService.initiatePayment("user@test.com", 1L, PaymentProvider.KASPI, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already been paid");
    }

    @Test
    void handleWebhook_stripeSuccess_promotesDraftToNewAndFiresEvents() {
        PaymentTransaction tx = PaymentTransaction.builder()
                .id(10L).order(order).provider(PaymentProvider.STRIPE)
                .status(PaymentStatus.PENDING).amount(BigDecimal.valueOf(500))
                .externalId("pi_abc").build();

        when(transactionRepository.findByExternalId("pi_abc")).thenReturn(Optional.of(tx));
        when(stripeProvider.isSuccessStatus("succeeded")).thenReturn(true);
        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));
        when(refOrderStatusRepository.findByCode("NEW")).thenReturn(Optional.of(newStatus));
        when(transactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentTransactionDto result = paymentService.handleWebhook(PaymentProvider.STRIPE, "pi_abc", "succeeded");

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(order.getStatus().getCode()).isEqualTo("NEW");
        verify(orderRepository).save(order);
        // Kitchen/print/board notifications fire only after payment promotes the draft.
        verify(eventPublisher, times(2)).publishEvent(any());
    }

    @Test
    void handleWebhook_stripeFailed_setsTransactionFailedAndKeepsDraft() {
        PaymentTransaction tx = PaymentTransaction.builder()
                .id(11L).order(order).provider(PaymentProvider.STRIPE)
                .status(PaymentStatus.PENDING).amount(BigDecimal.valueOf(500))
                .externalId("pi_abc").build();

        when(transactionRepository.findByExternalId("pi_abc")).thenReturn(Optional.of(tx));
        when(stripeProvider.isSuccessStatus("failed")).thenReturn(false);
        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order));
        when(transactionRepository.save(any(PaymentTransaction.class))).thenAnswer(inv -> inv.getArgument(0));

        PaymentTransactionDto result = paymentService.handleWebhook(PaymentProvider.STRIPE, "pi_abc", "failed");

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
        // Order stays an unpaid draft; no promotion, no board events.
        assertThat(order.getStatus().getCode()).isEqualTo("PENDING_PAYMENT");
        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
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
