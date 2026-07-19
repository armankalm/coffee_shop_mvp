package com.coffeeshop.app.controller;

import com.coffeeshop.app.dto.order.CreateOrderRequest;
import com.coffeeshop.app.dto.order.OrderDto;
import com.coffeeshop.app.service.OrderService;
import com.coffeeshop.app.service.board.OrderTrackingSseService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;
    private final OrderTrackingSseService orderTrackingSseService;

    public OrderController(OrderService orderService,
                           OrderTrackingSseService orderTrackingSseService) {
        this.orderService = orderService;
        this.orderTrackingSseService = orderTrackingSseService;
    }

    @PostMapping
    public ResponseEntity<OrderDto> createOrder(
            Authentication authentication,
            @Valid @RequestBody CreateOrderRequest request) {
        OrderDto order = orderService.createOrder(authentication.getName(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }

    @GetMapping
    public ResponseEntity<List<OrderDto>> getUserOrders(Authentication authentication) {
        return ResponseEntity.ok(orderService.getUserOrders(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDto> getOrderById(
            Authentication authentication,
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(orderService.getOrderById(authentication.getName(), id));
    }

    @PostMapping("/{id}/cancel")
    public ResponseEntity<OrderDto> cancelOrder(
            Authentication authentication,
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(orderService.cancelOrder(authentication.getName(), id));
    }

    /**
     * Live tracking of a single order (SSE). Emits an "order" event with the full
     * order on subscribe and on every status change. The access token is passed as
     * a query parameter because the browser EventSource API cannot send an
     * Authorization header. Access is authorized exactly like {@code GET /{id}}:
     * a regular USER may track only their own order; staff may track any.
     */
    @GetMapping(value = "/{id}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamOrder(
            Authentication authentication,
            @PathVariable("id") Long id) {
        // Throws NoSuchElement/AccessDenied if the order is missing or not visible to this user.
        orderService.getOrderById(authentication.getName(), id);
        return orderTrackingSseService.subscribe(id);
    }
}
