package com.coffeeshop.app.service.print;

import com.coffeeshop.app.domain.Order;
import com.coffeeshop.app.repository.OrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

/**
 * Listens for NewOrderEvent and automatically prints a receipt if auto-print is enabled.
 */
@Component
public class OrderPrintListener {

    private static final Logger log = LoggerFactory.getLogger(OrderPrintListener.class);

    private final PrintService printService;
    private final OrderRepository orderRepository;

    @Value("${print.auto-print.enabled:false}")
    private boolean autoPrintEnabled;

    public OrderPrintListener(PrintService printService, OrderRepository orderRepository) {
        this.printService = printService;
        this.orderRepository = orderRepository;
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNewOrder(NewOrderEvent event) {
        if (!autoPrintEnabled) {
            return;
        }
        Long orderId = event.getOrderId();
        log.info("Auto-printing receipt for new order #{}", orderId);
        try {
            Order order = orderRepository.findById(orderId)
                    .orElseThrow(() -> new IllegalStateException("Order not found for printing: " + orderId));
            printService.printReceipt(order);
        } catch (PrintException e) {
            log.error("Auto-print failed for order #{}: {}", orderId, e.getMessage());
        }
    }
}
