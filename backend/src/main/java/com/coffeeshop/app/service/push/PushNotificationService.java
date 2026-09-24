package com.coffeeshop.app.service.push;

import com.coffeeshop.app.domain.Order;
import com.coffeeshop.app.domain.PushSubscription;
import com.coffeeshop.app.domain.User;
import com.coffeeshop.app.repository.OrderRepository;
import com.coffeeshop.app.repository.PushSubscriptionRepository;
import com.coffeeshop.app.repository.UserRepository;
import com.coffeeshop.app.service.board.OrderStatusChangedEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;

import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

@Service
public class PushNotificationService {

    private static final Logger log = LoggerFactory.getLogger(PushNotificationService.class);

    /** An order stays "ready" for minutes, not days: stale notifications are dropped. */
    private static final int READY_TTL_SECONDS = 60 * 60;

    /**
     * The server POSTs to subscription endpoints, so only the browsers' push services are
     * accepted (Chrome/Edge via FCM, Firefox, Safari, legacy Edge) rather than any URL.
     */
    private static final Set<String> PUSH_SERVICE_DOMAINS = Set.of(
            "fcm.googleapis.com", "push.services.mozilla.com", "push.apple.com", "notify.windows.com");

    private final PushSubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final WebPushSender sender;
    private final ObjectMapper objectMapper;

    public PushNotificationService(PushSubscriptionRepository subscriptionRepository,
                                   UserRepository userRepository,
                                   OrderRepository orderRepository,
                                   WebPushSender sender,
                                   ObjectMapper objectMapper) {
        this.subscriptionRepository = subscriptionRepository;
        this.userRepository = userRepository;
        this.orderRepository = orderRepository;
        this.sender = sender;
        this.objectMapper = objectMapper;
    }

    public boolean isEnabled() {
        return sender.isEnabled();
    }

    public String getPublicKey() {
        return sender.getPublicKey();
    }

    @Transactional
    public void subscribe(String userEmail, String endpoint, String p256dh, String auth) {
        validateEndpoint(endpoint);
        validateKeys(p256dh, auth);
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + userEmail));

        PushSubscription subscription = subscriptionRepository.findByEndpoint(endpoint)
                .orElseGet(() -> PushSubscription.builder().endpoint(endpoint).build());
        subscription.setUser(user);
        subscription.setP256dh(p256dh);
        subscription.setAuth(auth);
        subscriptionRepository.save(subscription);
    }

    @Transactional
    public void unsubscribe(String userEmail, String endpoint) {
        subscriptionRepository.findByEndpoint(endpoint)
                .filter(subscription -> subscription.getUser().getEmail().equalsIgnoreCase(userEmail))
                .ifPresent(subscriptionRepository::delete);
    }

    /**
     * Runs after the status change commits and off the request thread, so a slow push
     * service never delays or rolls back the barista's action.
     */
    @Async
    @TransactionalEventListener(fallbackExecution = true)
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        if (event.getOrderId() == null || !isEnabled()) {
            return;
        }
        orderRepository.findByIdWithDetails(event.getOrderId())
                .filter(order -> "READY".equals(order.getStatus().getCode()))
                .ifPresent(this::notifyOrderReady);
    }

    void notifyOrderReady(Order order) {
        List<PushSubscription> subscriptions = subscriptionRepository.findByUserId(order.getUser().getId());
        if (subscriptions.isEmpty()) {
            return;
        }
        byte[] payload = readyPayload(order);
        int sent = 0;
        int gone = 0;
        for (PushSubscription subscription : subscriptions) {
            WebPushSender.Result result = sender.send(subscription, payload, READY_TTL_SECONDS);
            if (result == WebPushSender.Result.SENT) {
                sent++;
            } else if (result == WebPushSender.Result.GONE) {
                gone++;
                subscriptionRepository.delete(subscription);
            }
        }
        log.info("Order {} ready: push delivered to {} of {} subscription(s), {} expired and removed",
                order.getId(), sent, subscriptions.size(), gone);
    }

    private byte[] readyPayload(Order order) {
        String number = order.getDailyNumber() != null ? String.valueOf(order.getDailyNumber()) : String.valueOf(order.getId());
        Map<String, String> message = new LinkedHashMap<>();
        message.put("title", "Заказ №" + number + " готов");
        message.put("body", "Можно забирать: " + order.getShop().getName());
        message.put("url", "/order/" + order.getId());
        message.put("tag", "order-" + order.getId());
        try {
            return objectMapper.writeValueAsBytes(message);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void validateEndpoint(String endpoint) {
        URI uri;
        try {
            uri = URI.create(endpoint);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid push endpoint");
        }
        String host = uri.getHost() == null ? "" : uri.getHost().toLowerCase(Locale.ROOT);
        boolean knownService = PUSH_SERVICE_DOMAINS.stream()
                .anyMatch(domain -> host.equals(domain) || host.endsWith("." + domain));
        if (!"https".equals(uri.getScheme()) || !knownService) {
            throw new IllegalArgumentException("Unsupported push endpoint");
        }
    }

    private static void validateKeys(String p256dh, String auth) {
        try {
            WebPushCrypto.decodePublicKey(WebPushCrypto.base64UrlDecode(p256dh));
            if (WebPushCrypto.base64UrlDecode(auth).length != 16) {
                throw new IllegalArgumentException("Invalid push auth secret");
            }
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid push subscription keys");
        }
    }
}
