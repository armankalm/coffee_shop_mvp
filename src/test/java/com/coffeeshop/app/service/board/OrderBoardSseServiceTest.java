package com.coffeeshop.app.service.board;

import com.coffeeshop.app.domain.*;
import com.coffeeshop.app.dto.order.OrderBoardEntryDto;
import com.coffeeshop.app.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderBoardSseServiceTest {

    @Mock private OrderRepository orderRepository;

    @InjectMocks
    private OrderBoardSseService service;

    private RefOrderStatus status(String code) {
        return RefOrderStatus.builder().id(1L).code(code).nameRu(code).nameEn(code).build();
    }

    private User user(String name) {
        return User.builder().id(1L).email("c@e.com").name(name)
                .role(RefUserRole.builder().id(1L).code("USER").nameRu("U").nameEn("U").build())
                .build();
    }

    private Order order(long id, String statusCode, String customerName, Instant createdAt) {
        return Order.builder()
                .id(id)
                .user(user(customerName))
                .shop(CoffeeShop.builder().id(7L).name("Shop").build())
                .status(status(statusCode))
                .total(BigDecimal.TEN)
                .createdAt(createdAt)
                .build();
    }

    @Test
    void currentBoard_keepsOnlyInProgressAndReady_sortedOldestFirst() {
        Order ready = order(2L, "READY", "Анна", Instant.parse("2026-07-19T08:05:00Z"));
        Order inProgress = order(1L, "IN_PROGRESS", "Гость", Instant.parse("2026-07-19T08:00:00Z"));
        Order newOrder = order(3L, "NEW", "Иван", Instant.parse("2026-07-19T08:01:00Z"));
        Order completed = order(4L, "COMPLETED", "Пётр", Instant.parse("2026-07-19T07:00:00Z"));

        when(orderRepository.findByShopIdWithDetails(7L))
                .thenReturn(List.of(ready, inProgress, newOrder, completed));

        List<OrderBoardEntryDto> board = service.currentBoard(7L);

        // NEW and COMPLETED are excluded; remaining sorted oldest-first.
        assertThat(board).extracting(OrderBoardEntryDto::getOrderId).containsExactly(1L, 2L);
        assertThat(board).extracting(OrderBoardEntryDto::getStatus).containsExactly("IN_PROGRESS", "READY");
        assertThat(board.get(0).getCustomerName()).isEqualTo("Гость");
    }

    @Test
    void currentBoard_fallsBackToGuestWhenNameMissing() {
        Order inProgress = order(1L, "IN_PROGRESS", null, Instant.parse("2026-07-19T08:00:00Z"));
        when(orderRepository.findByShopIdWithDetails(7L)).thenReturn(List.of(inProgress));

        List<OrderBoardEntryDto> board = service.currentBoard(7L);

        assertThat(board).hasSize(1);
        assertThat(board.get(0).getCustomerName()).isEqualTo("Гость");
    }
}
