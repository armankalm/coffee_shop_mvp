package com.coffeeshop.app.service.print;

import com.coffeeshop.app.domain.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderPrintListenerTest {

    @Mock
    private PrintService printService;

    @InjectMocks
    private OrderPrintListener listener;

    private Order buildOrder() {
        User user = User.builder().id(1L).email("test@test.com").role(Role.USER).build();
        CoffeeShop shop = CoffeeShop.builder().id(1L).name("Shop").city("City")
                .address("Addr").status(ShopStatus.OPEN).build();
        Order order = Order.builder()
                .id(1L).user(user).shop(shop)
                .status(OrderStatus.NEW).total(BigDecimal.valueOf(500)).items(List.of())
                .build();
        order.setCreatedAt(Instant.now());
        return order;
    }

    @Test
    void onNewOrder_autoPrintEnabled_callsPrint() {
        ReflectionTestUtils.setField(listener, "autoPrintEnabled", true);
        Order order = buildOrder();
        NewOrderEvent event = new NewOrderEvent(this, order);

        listener.onNewOrder(event);

        verify(printService).printReceipt(order);
    }

    @Test
    void onNewOrder_autoPrintDisabled_doesNotPrint() {
        ReflectionTestUtils.setField(listener, "autoPrintEnabled", false);
        Order order = buildOrder();
        NewOrderEvent event = new NewOrderEvent(this, order);

        listener.onNewOrder(event);

        verifyNoInteractions(printService);
    }

    @Test
    void onNewOrder_printFails_doesNotThrow() {
        ReflectionTestUtils.setField(listener, "autoPrintEnabled", true);
        Order order = buildOrder();
        NewOrderEvent event = new NewOrderEvent(this, order);
        doThrow(new PrintException("Printer offline")).when(printService).printReceipt(order);

        // Should not propagate exception
        listener.onNewOrder(event);

        verify(printService).printReceipt(order);
    }
}
