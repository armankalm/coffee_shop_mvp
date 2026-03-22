package com.coffeeshop.app.service.print;

import com.coffeeshop.app.domain.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

/**
 * Listens for NewOrderEvent and automatically prints a receipt if auto-print is enabled.
 */
@Component
public class OrderPrintListener {

    private static final Logger log = LoggerFactory.getLogger(OrderPrintListener.class);

    private final PrintService printService;

    @Value("${print.auto-print.enabled:false}")
    private boolean autoPrintEnabled;

    public OrderPrintListener(PrintService printService) {
        this.printService = printService;
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNewOrder(NewOrderEvent event) {
        if (!autoPrintEnabled) {
            return;
        }
        Order order = event.getOrder();
        log.info("Auto-printing receipt for new order #{}", order.getId());
        try {
            printService.printReceipt(order);
        } catch (PrintException e) {
            log.error("Auto-print failed for order #{}: {}", order.getId(), e.getMessage());
        }
    }
}
