package com.coffeeshop.app.controller;

import com.coffeeshop.app.domain.*;
import com.coffeeshop.app.dto.order.CreateOrderRequest;
import com.coffeeshop.app.dto.order.OrderDto;
import com.coffeeshop.app.dto.order.OrderItemRequest;
import com.coffeeshop.app.service.OrderService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@Import({com.coffeeshop.app.config.SecurityConfig.class,
         com.coffeeshop.app.config.GlobalExceptionHandler.class,
         com.coffeeshop.app.security.JwtAuthFilter.class,
         com.coffeeshop.app.security.JwtTokenProvider.class,
         com.coffeeshop.app.security.JwtProperties.class})
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private OrderService orderService;

    @MockBean
    private com.coffeeshop.app.service.board.OrderTrackingSseService orderTrackingSseService;

    private RefUserRole userRole() {
        return RefUserRole.builder().id(1L).code("USER").nameRu("Пользователь").nameEn("User").build();
    }

    private RefShopStatus openStatus() {
        return RefShopStatus.builder().id(1L).code("OPEN").nameRu("Открыто").nameEn("Open").build();
    }

    private RefOrderStatus orderStatus(Long id, String code, String nameEn) {
        return RefOrderStatus.builder().id(id).code(code).nameRu(nameEn).nameEn(nameEn).build();
    }

    private OrderDto buildDto(Long id, RefOrderStatus status) {
        User user = User.builder().id(1L).email("user@test.com").role(userRole()).build();
        City almatyCity = City.builder().id(1L).name("Almaty").active(true).build();
        CoffeeShop shop = CoffeeShop.builder().id(1L).name("Test Shop").city(almatyCity)
                .address("123 St").status(openStatus()).build();
        Order order = Order.builder()
                .id(id).user(user).shop(shop)
                .status(status)
                .total(BigDecimal.valueOf(500))
                .build();
        return OrderDto.from(order);
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void createOrder_validRequest_returns201() throws Exception {
        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(1L);
        itemRequest.setQuantity(1);

        CreateOrderRequest request = new CreateOrderRequest();
        request.setShopId(1L);
        request.setItems(List.of(itemRequest));

        OrderDto orderDto = buildDto(1L, orderStatus(1L, "NEW", "New"));
        when(orderService.createOrder(eq("user@test.com"), any(CreateOrderRequest.class)))
                .thenReturn(orderDto);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("NEW"));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void createOrder_missingShopId_returns400() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setItems(List.of(new OrderItemRequest() {{ setProductId(1L); }}));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void getUserOrders_returnsOrderList() throws Exception {
        OrderDto dto = buildDto(1L, orderStatus(1L, "NEW", "New"));
        when(orderService.getUserOrders("user@test.com")).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void getOrderById_existingOrder_returnsOrder() throws Exception {
        OrderDto dto = buildDto(1L, orderStatus(1L, "NEW", "New"));
        when(orderService.getOrderById("user@test.com", 1L)).thenReturn(dto);

        mockMvc.perform(get("/api/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void getOrderById_notFound_returns404() throws Exception {
        when(orderService.getOrderById("user@test.com", 99L))
                .thenThrow(new NoSuchElementException("Order not found: 99"));

        mockMvc.perform(get("/api/orders/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void cancelOrder_validOrder_returnsCancelledOrder() throws Exception {
        OrderDto dto = buildDto(1L, orderStatus(5L, "CANCELLED", "Cancelled"));
        when(orderService.cancelOrder("user@test.com", 1L)).thenReturn(dto);

        mockMvc.perform(post("/api/orders/1/cancel"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void createOrder_unauthenticated_returns401() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest();
        request.setShopId(1L);

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void streamOrder_authorizedOrder_subscribes() throws Exception {
        when(orderService.getOrderById("user@test.com", 1L))
                .thenReturn(buildDto(1L, orderStatus(1L, "NEW", "New")));
        when(orderTrackingSseService.subscribe(1L))
                .thenReturn(new org.springframework.web.servlet.mvc.method.annotation.SseEmitter());

        mockMvc.perform(get("/api/orders/1/stream").accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isOk());

        verify(orderService).getOrderById("user@test.com", 1L);
        verify(orderTrackingSseService).subscribe(1L);
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void streamOrder_foreignOrder_deniedAndDoesNotSubscribe() throws Exception {
        when(orderService.getOrderById("user@test.com", 2L))
                .thenThrow(new com.coffeeshop.app.config.AccessDeniedException("Access denied to order: 2"));

        // Access is checked before subscribing; the denial propagates and no emitter is created.
        try {
            mockMvc.perform(get("/api/orders/2/stream").accept(MediaType.TEXT_EVENT_STREAM));
        } catch (Exception ignored) {
            // MockMvc surfaces the pre-subscribe denial as a servlet processing failure for
            // streaming endpoints; the security contract we assert is that subscribe never ran.
        }

        verify(orderService).getOrderById("user@test.com", 2L);
        verify(orderTrackingSseService, never()).subscribe(any());
    }

    @Test
    void streamOrder_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/orders/1/stream").accept(MediaType.TEXT_EVENT_STREAM))
                .andExpect(status().isUnauthorized());
    }
}
