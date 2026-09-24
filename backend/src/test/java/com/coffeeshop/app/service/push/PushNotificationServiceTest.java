package com.coffeeshop.app.service.push;

import com.coffeeshop.app.domain.CoffeeShop;
import com.coffeeshop.app.domain.Order;
import com.coffeeshop.app.domain.PushSubscription;
import com.coffeeshop.app.domain.RefOrderStatus;
import com.coffeeshop.app.domain.User;
import com.coffeeshop.app.repository.OrderRepository;
import com.coffeeshop.app.repository.PushSubscriptionRepository;
import com.coffeeshop.app.repository.UserRepository;
import com.coffeeshop.app.service.board.OrderStatusChangedEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.interfaces.ECPublicKey;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PushNotificationServiceTest {

    private static final String AUTH_SECRET = "BTBZMqHH6r4Tts7J_aSIgg";

    @Mock private PushSubscriptionRepository subscriptionRepository;
    @Mock private UserRepository userRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private WebPushSender sender;

    private PushNotificationService service;
    private final User user = User.builder().id(5L).email("guest@example.com").build();

    @BeforeEach
    void setUp() {
        service = new PushNotificationService(subscriptionRepository, userRepository, orderRepository, sender, new ObjectMapper());
    }

    private Order order(String statusCode) {
        return Order.builder()
                .id(42L)
                .dailyNumber(7)
                .user(user)
                .shop(CoffeeShop.builder().id(1L).name("Центр").build())
                .status(RefOrderStatus.builder().code(statusCode).build())
                .build();
    }

    private PushSubscription subscription(String endpoint) {
        return PushSubscription.builder().id(1L).user(user).endpoint(endpoint).p256dh("k").auth("a").build();
    }

    @Test
    void readyOrder_isPushedToEverySubscriptionOfItsOwner() {
        when(sender.isEnabled()).thenReturn(true);
        when(orderRepository.findByIdWithDetails(42L)).thenReturn(Optional.of(order("READY")));
        PushSubscription phone = subscription("https://fcm.googleapis.com/fcm/send/1");
        when(subscriptionRepository.findByUserId(5L)).thenReturn(List.of(phone));
        when(sender.send(eq(phone), any(), anyInt())).thenReturn(WebPushSender.Result.SENT);

        service.onOrderStatusChanged(new OrderStatusChangedEvent(this, 1L, 42L));

        ArgumentCaptor<byte[]> payload = ArgumentCaptor.forClass(byte[].class);
        verify(sender).send(eq(phone), payload.capture(), anyInt());
        assertThat(new String(payload.getValue(), StandardCharsets.UTF_8))
                .contains("Заказ №7 готов")
                .contains("\"url\":\"/order/42\"");
    }

    @Test
    void otherStatuses_areNotPushed() {
        when(sender.isEnabled()).thenReturn(true);
        when(orderRepository.findByIdWithDetails(42L)).thenReturn(Optional.of(order("IN_PROGRESS")));

        service.onOrderStatusChanged(new OrderStatusChangedEvent(this, 1L, 42L));

        verify(sender, never()).send(any(), any(), anyInt());
    }

    @Test
    void goneSubscriptions_areRemoved() {
        when(sender.isEnabled()).thenReturn(true);
        when(orderRepository.findByIdWithDetails(42L)).thenReturn(Optional.of(order("READY")));
        PushSubscription stale = subscription("https://fcm.googleapis.com/fcm/send/old");
        when(subscriptionRepository.findByUserId(5L)).thenReturn(List.of(stale));
        when(sender.send(eq(stale), any(), anyInt())).thenReturn(WebPushSender.Result.GONE);

        service.onOrderStatusChanged(new OrderStatusChangedEvent(this, 1L, 42L));

        verify(subscriptionRepository).delete(stale);
    }

    @Test
    void subscribe_rejectsEndpointsOutsideKnownPushServices() throws Exception {
        String key = validKey();

        assertThatThrownBy(() -> service.subscribe("guest@example.com", "https://evil.example.com/x", key, AUTH_SECRET))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.subscribe("guest@example.com", "http://fcm.googleapis.com/x", key, AUTH_SECRET))
                .isInstanceOf(IllegalArgumentException.class);
        verify(subscriptionRepository, never()).save(any());
    }

    @Test
    void subscribe_savesValidSubscriptionForTheUser() throws Exception {
        when(userRepository.findByEmail("guest@example.com")).thenReturn(Optional.of(user));
        String endpoint = "https://web.push.apple.com/abc";
        when(subscriptionRepository.findByEndpoint(endpoint)).thenReturn(Optional.empty());

        service.subscribe("guest@example.com", endpoint, validKey(), AUTH_SECRET);

        ArgumentCaptor<PushSubscription> saved = ArgumentCaptor.forClass(PushSubscription.class);
        verify(subscriptionRepository).save(saved.capture());
        assertThat(saved.getValue().getUser()).isEqualTo(user);
        assertThat(saved.getValue().getEndpoint()).isEqualTo(endpoint);
    }

    private static String validKey() throws Exception {
        KeyPair pair = WebPushCrypto.generateKeyPair();
        return WebPushCrypto.base64UrlEncode(WebPushCrypto.encodePublicKey((ECPublicKey) pair.getPublic()));
    }
}
