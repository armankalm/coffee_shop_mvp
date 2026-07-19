package com.coffeeshop.app.service.board;

import com.coffeeshop.app.domain.Order;
import com.coffeeshop.app.dto.order.OrderBoardEntryDto;
import com.coffeeshop.app.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Keeps a set of SSE subscribers per shop and pushes the pickup board
 * (IN_PROGRESS + READY orders) whenever an order status changes.
 */
@Service
public class OrderBoardSseService {

    private static final Logger log = LoggerFactory.getLogger(OrderBoardSseService.class);
    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L; // 30 min
    private static final Set<String> BOARD_STATUSES = Set.of("IN_PROGRESS", "READY");

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emittersByShop = new ConcurrentHashMap<>();
    private final OrderRepository orderRepository;

    public OrderBoardSseService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    /**
     * Registers a new subscriber for a shop and immediately sends the current snapshot.
     */
    @Transactional(readOnly = true)
    public SseEmitter subscribe(Long shopId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        CopyOnWriteArrayList<SseEmitter> emitters =
                emittersByShop.computeIfAbsent(shopId, key -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);

        emitter.onCompletion(() -> removeEmitter(shopId, emitter));
        emitter.onTimeout(() -> removeEmitter(shopId, emitter));
        emitter.onError(error -> removeEmitter(shopId, emitter));

        sendSnapshot(emitter, shopId);
        return emitter;
    }

    @EventListener
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        broadcast(event.getShopId());
    }

    /** Reads the current board for a shop, sorted oldest-first (FIFO). */
    @Transactional(readOnly = true)
    public List<OrderBoardEntryDto> currentBoard(Long shopId) {
        return orderRepository.findByShopIdWithDetails(shopId).stream()
                .filter(order -> BOARD_STATUSES.contains(order.getStatus().getCode()))
                .sorted(Comparator.comparing(Order::getCreatedAt))
                .map(OrderBoardEntryDto::from)
                .collect(Collectors.toList());
    }

    private void broadcast(Long shopId) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByShop.get(shopId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        List<OrderBoardEntryDto> board = currentBoard(shopId);
        for (SseEmitter emitter : emitters) {
            sendBoard(emitter, shopId, board);
        }
    }

    private void sendSnapshot(SseEmitter emitter, Long shopId) {
        sendBoard(emitter, shopId, currentBoard(shopId));
    }

    private void sendBoard(SseEmitter emitter, Long shopId, List<OrderBoardEntryDto> board) {
        try {
            emitter.send(SseEmitter.event().name("board").data(board));
        } catch (IOException | IllegalStateException ex) {
            log.debug("Removing dead SSE emitter for shop {}: {}", shopId, ex.getMessage());
            removeEmitter(shopId, emitter);
        }
    }

    private void removeEmitter(Long shopId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByShop.get(shopId);
        if (emitters != null) {
            emitters.remove(emitter);
        }
    }
}
