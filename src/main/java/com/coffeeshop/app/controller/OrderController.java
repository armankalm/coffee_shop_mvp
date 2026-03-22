package com.coffeeshop.app.controller;

import com.coffeeshop.app.dto.order.CreateOrderRequest;
import com.coffeeshop.app.dto.order.OrderDto;
import com.coffeeshop.app.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
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
}
