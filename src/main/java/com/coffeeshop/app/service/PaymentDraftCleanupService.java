package com.coffeeshop.app.service;

import com.coffeeshop.app.domain.Order;
import com.coffeeshop.app.domain.RefOrderStatus;
import com.coffeeshop.app.repository.OrderRepository;
import com.coffeeshop.app.repository.RefOrderStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

/**
 * Periodically cancels unpaid PENDING_PAYMENT drafts older than the configured TTL so the
 * database does not accumulate abandoned checkouts.
 */
@Service
public class PaymentDraftCleanupService {

    private static final Logger log = LoggerFactory.getLogger(PaymentDraftCleanupService.class);

    private final OrderRepository orderRepository;
    private final RefOrderStatusRepository refOrderStatusRepository;
    private final long ttlMinutes;

    public PaymentDraftCleanupService(OrderRepository orderRepository,
                                      RefOrderStatusRepository refOrderStatusRepository,
                                      @Value("${app.payment.draft-ttl-minutes:30}") long ttlMinutes) {
        this.orderRepository = orderRepository;
        this.refOrderStatusRepository = refOrderStatusRepository;
        this.ttlMinutes = ttlMinutes;
    }

    // Runs every 5 minutes; the TTL itself is configurable via app.payment.draft-ttl-minutes.
    @Scheduled(fixedDelayString = "${app.payment.draft-cleanup-interval-ms:300000}")
    @Transactional
    public void cancelExpiredDrafts() {
        Optional<RefOrderStatus> pending = refOrderStatusRepository.findByCode("PENDING_PAYMENT");
        Optional<RefOrderStatus> cancelled = refOrderStatusRepository.findByCode("CANCELLED");
        if (pending.isEmpty() || cancelled.isEmpty()) {
            return;
        }

        Instant cutoff = Instant.now().minus(ttlMinutes, ChronoUnit.MINUTES);
        List<Order> expired = orderRepository.findByStatusAndCreatedAtBefore(pending.get(), cutoff);
        if (expired.isEmpty()) {
            return;
        }

        for (Order order : expired) {
            order.setStatus(cancelled.get());
        }
        orderRepository.saveAll(expired);
        log.info("Auto-cancelled {} expired PENDING_PAYMENT draft(s) older than {} minutes", expired.size(), ttlMinutes);
    }
}
