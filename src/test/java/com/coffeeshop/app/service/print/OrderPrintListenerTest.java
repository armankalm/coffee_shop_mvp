package com.coffeeshop.app.service.print;

import com.coffeeshop.app.domain.*;
import com.coffeeshop.app.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderPrintListenerTest {

    @Mock
    private PrintService printService;

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderPrintListener listener;

    private Order buildOrder() {
        RefUserRole userRole = RefUserRole.builder().id(1L).code("USER").nameRu("Пользователь").nameEn("User").build();
        RefShopStatus openStatus = RefShopStatus.builder().id(1L).code("OPEN").nameRu("Открыто").nameEn("Open").build();
        RefOrderStatus newStatus = RefOrderStatus.builder().id(1L).code("NEW").nameRu("Новый").nameEn("New").build();

        User user = User.builder().id(1L).email("test@test.com").role(userRole).build();
        City shopCity = City.builder().id(1L).name("City").active(true).build();
        CoffeeShop shop = CoffeeShop.builder().id(1L).name("Shop").city(shopCity)
                .address("Addr").status(openStatus).build();
        Order order = Order.builder()
                .id(1L).user(user).shop(shop)
                .status(newStatus).total(BigDecimal.valueOf(500)).items(Set.of())
                .build();
        order.setCreatedAt(Instant.now());
        return order;
    }

    @Test
    void onNewOrder_autoPrintEnabled_callsPrint() {
        ReflectionTestUtils.setField(listener, "autoPrintEnabled", true);
        Order order = buildOrder();
        NewOrderEvent event = new NewOrderEvent(this, order.getId());
        when(orderRepository.findByIdWithDetails(order.getId())).thenReturn(Optional.of(order));

        listener.onNewOrder(event);

        verify(printService).printReceipt(order);
    }

    @Test
    void onNewOrder_autoPrintDisabled_doesNotPrint() {
        ReflectionTestUtils.setField(listener, "autoPrintEnabled", false);
        Order order = buildOrder();
        NewOrderEvent event = new NewOrderEvent(this, order.getId());

        listener.onNewOrder(event);

        verifyNoInteractions(printService);
    }

    @Test
    void onNewOrder_printFails_doesNotThrow() {
        ReflectionTestUtils.setField(listener, "autoPrintEnabled", true);
        Order order = buildOrder();
        NewOrderEvent event = new NewOrderEvent(this, order.getId());
        when(orderRepository.findByIdWithDetails(order.getId())).thenReturn(Optional.of(order));
        doThrow(new PrintException("Printer offline")).when(printService).printReceipt(order);

        // Should not propagate exception
        listener.onNewOrder(event);

        verify(printService).printReceipt(order);
    }
}
