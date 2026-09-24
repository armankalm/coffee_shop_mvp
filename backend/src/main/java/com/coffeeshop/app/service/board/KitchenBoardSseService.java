package com.coffeeshop.app.service.board;

import com.coffeeshop.app.domain.OrderItem;
import com.coffeeshop.app.dto.order.OrderItemBoardDto;
import com.coffeeshop.app.repository.OrderItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * Keeps SSE subscribers per shop for the staff kitchen board and pushes the
 * shop's active order items whenever any order/item status changes.
 *
 * The kitchen board tracks items individually (NEW → IN_PROGRESS → READY),
 * unlike {@link OrderBoardSseService} which serves the customer pickup board.
 */
@Service
public class KitchenBoardSseService {

    private static final Logger log = LoggerFactory.getLogger(KitchenBoardSseService.class);
    private static final long SSE_TIMEOUT_MS = 30 * 60 * 1000L; // 30 min

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emittersByShop = new ConcurrentHashMap<>();
    private final OrderItemRepository orderItemRepository;

    public KitchenBoardSseService(OrderItemRepository orderItemRepository) {
        this.orderItemRepository = orderItemRepository;
    }

    @Transactional(readOnly = true)
    public SseEmitter subscribe(Long shopId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        CopyOnWriteArrayList<SseEmitter> emitters =
                emittersByShop.computeIfAbsent(shopId, key -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);

        emitter.onCompletion(() -> removeEmitter(shopId, emitter));
        emitter.onTimeout(() -> removeEmitter(shopId, emitter));
        emitter.onError(error -> removeEmitter(shopId, emitter));

        sendItems(emitter, shopId, currentItems(shopId));
        return emitter;
    }

    // AFTER_COMMIT so newly created/updated items are already visible in the DB.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        broadcast(event.getShopId());
    }

    @Transactional(readOnly = true)
    public List<OrderItemBoardDto> currentItems(Long shopId) {
        return orderItemRepository.findByShopIdInWithDetails(Set.of(shopId)).stream()
                .map(OrderItemBoardDto::from)
                .collect(Collectors.toList());
    }

    private void broadcast(Long shopId) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByShop.get(shopId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        List<OrderItemBoardDto> items = currentItems(shopId);
        for (SseEmitter emitter : emitters) {
            sendItems(emitter, shopId, items);
        }
    }

    private void sendItems(SseEmitter emitter, Long shopId, List<OrderItemBoardDto> items) {
        try {
            emitter.send(SseEmitter.event().name("items").data(items));
        } catch (IOException | IllegalStateException ex) {
            log.debug("Removing dead kitchen SSE emitter for shop {}: {}", shopId, ex.getMessage());
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
