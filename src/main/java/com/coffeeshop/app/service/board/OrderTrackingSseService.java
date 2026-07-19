package com.coffeeshop.app.service.board;

import com.coffeeshop.app.dto.order.OrderDto;
import com.coffeeshop.app.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Per-order SSE channel: a customer (or POS terminal) subscribes to a single order
 * and receives the full {@link OrderDto} whenever that order changes — on creation
 * and on every status transition (NEW → IN_PROGRESS → READY → COMPLETED/CANCELLED).
 */
@Service
public class OrderTrackingSseService {

    private static final Logger log = LoggerFactory.getLogger(OrderTrackingSseService.class);
    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L; // 30 min

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emittersByOrder = new ConcurrentHashMap<>();
    private final OrderRepository orderRepository;

    public OrderTrackingSseService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Registers a subscriber for a single order and immediately sends the current snapshot.
     */
    @Transactional(readOnly = true)
    public SseEmitter subscribe(Long orderId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        CopyOnWriteArrayList<SseEmitter> emitters =
                emittersByOrder.computeIfAbsent(orderId, key -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);

        emitter.onCompletion(() -> removeEmitter(orderId, emitter));
        emitter.onTimeout(() -> removeEmitter(orderId, emitter));
        emitter.onError(error -> removeEmitter(orderId, emitter));

        sendSnapshot(emitter, orderId);
        return emitter;
    }

    @EventListener
    public void onOrderChanged(OrderStatusChangedEvent event) {
        if (event.getOrderId() == null) {
            return;
        }
        broadcast(event.getOrderId());
    }

    private void broadcast(Long orderId) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByOrder.get(orderId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        OrderDto dto = currentOrder(orderId);
        if (dto == null) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            sendOrder(orderId, emitter, dto);
        }
    }

    @Transactional(readOnly = true)
    public OrderDto currentOrder(Long orderId) {
        return orderRepository.findByIdWithDetails(orderId)
                .map(OrderDto::from)
                .orElse(null);
    }

    private void sendSnapshot(SseEmitter emitter, Long orderId) {
        OrderDto dto = currentOrder(orderId);
        if (dto != null) {
            sendOrder(orderId, emitter, dto);
        }
    }

    private void sendOrder(Long orderId, SseEmitter emitter, OrderDto dto) {
        try {
            emitter.send(SseEmitter.event().name("order").data(dto));
        } catch (IOException | IllegalStateException ex) {
            log.debug("Removing dead SSE emitter for order {}: {}", orderId, ex.getMessage());
            removeEmitter(orderId, emitter);
        }
    }

    private void removeEmitter(Long orderId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByOrder.get(orderId);
        if (emitters != null) {
            emitters.remove(emitter);
        }
    }
}
