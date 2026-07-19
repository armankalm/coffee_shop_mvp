package com.coffeeshop.app.service.board;

import com.coffeeshop.app.domain.*;
import com.coffeeshop.app.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderTrackingSseServiceTest {

    @Mock private OrderRepository orderRepository;

    @InjectMocks
    private OrderTrackingSseService service;

    private Order order(long id, String statusCode) {
        return Order.builder()
                .id(id)
                .user(User.builder().id(1L).email("c@e.com").name("Иван")
                        .role(RefUserRole.builder().id(1L).code("USER").nameRu("U").nameEn("U").build())
                        .build())
                .shop(CoffeeShop.builder().id(7L).name("Shop").build())
                .status(RefOrderStatus.builder().id(1L).code(statusCode).nameRu(statusCode).nameEn(statusCode).build())
                .total(BigDecimal.TEN)
                .createdAt(Instant.parse("2026-07-19T08:00:00Z"))
                .build();
    }

    @Test
    void subscribe_sendsCurrentSnapshotImmediately() {
        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order(1L, "NEW")));

        service.subscribe(1L);

        // Snapshot read on subscribe.
        verify(orderRepository).findByIdWithDetails(1L);
    }

    @Test
    void onOrderChanged_withNoSubscribers_doesNotQueryOrder() {
        // No one subscribed to order 1 → event for it must not hit the DB.
        service.onOrderChanged(new OrderStatusChangedEvent(this, 7L, 1L));

        verify(orderRepository, never()).findByIdWithDetails(anyLong());
    }

    @Test
    void onOrderChanged_nullOrderId_isIgnored() {
        service.onOrderChanged(new OrderStatusChangedEvent(this, 7L, null));

        verify(orderRepository, never()).findByIdWithDetails(anyLong());
    }

    @Test
    void onOrderChanged_broadcastsToSubscribersOfThatOrder() {
        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order(1L, "NEW")));
        service.subscribe(1L); // one read on subscribe

        when(orderRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(order(1L, "IN_PROGRESS")));
        service.onOrderChanged(new OrderStatusChangedEvent(this, 7L, 1L)); // second read on broadcast

        verify(orderRepository, times(2)).findByIdWithDetails(1L);
    }
}
