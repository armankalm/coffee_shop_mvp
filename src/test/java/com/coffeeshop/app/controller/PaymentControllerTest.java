package com.coffeeshop.app.controller;

import com.coffeeshop.app.domain.*;
import com.coffeeshop.app.dto.payment.PaymentRequest;
import com.coffeeshop.app.dto.payment.PaymentTransactionDto;
import com.coffeeshop.app.dto.payment.WebhookPayload;
import com.coffeeshop.app.service.PaymentService;
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
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PaymentController.class)
@Import({com.coffeeshop.app.config.SecurityConfig.class,
         com.coffeeshop.app.config.GlobalExceptionHandler.class,
         com.coffeeshop.app.security.JwtAuthFilter.class,
         com.coffeeshop.app.security.JwtTokenProvider.class,
         com.coffeeshop.app.security.JwtProperties.class})
class PaymentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PaymentService paymentService;

    private PaymentTransactionDto buildTx(PaymentProvider provider, PaymentStatus status) {
        RefUserRole userRole = RefUserRole.builder().id(1L).code("USER").nameRu("Пользователь").nameEn("User").build();
        RefShopStatus openStatus = RefShopStatus.builder().id(1L).code("OPEN").nameRu("Открыто").nameEn("Open").build();
        RefOrderStatus newStatus = RefOrderStatus.builder().id(1L).code("NEW").nameRu("Новый").nameEn("New").build();

        User user = User.builder().id(1L).email("user@test.com").role(userRole).build();
        CoffeeShop shop = CoffeeShop.builder()
                .id(1L).name("Shop").city("City").address("Addr").status(openStatus).build();
        Order order = Order.builder()
                .id(1L).user(user).shop(shop)
                .status(newStatus)
                .total(BigDecimal.valueOf(500)).build();
        PaymentTransaction tx = PaymentTransaction.builder()
                .id(10L).order(order).provider(provider).status(status)
                .amount(BigDecimal.valueOf(500)).externalId("EXT-001")
                .build();
        tx.setCreatedAt(Instant.now());
        tx.setUpdatedAt(Instant.now());
        return PaymentTransactionDto.from(tx);
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void pay_validRequest_returns200WithPendingTx() throws Exception {
        PaymentTransactionDto txDto = buildTx(PaymentProvider.KASPI, PaymentStatus.PENDING);
        when(paymentService.initiatePayment(eq("user@test.com"), eq(1L), eq(PaymentProvider.KASPI)))
                .thenReturn(txDto);

        PaymentRequest req = new PaymentRequest();
        req.setProvider("KASPI");

        mockMvc.perform(post("/api/orders/1/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.provider").value("KASPI"));
    }

    @Test
    void pay_unauthenticated_returns401() throws Exception {
        PaymentRequest req = new PaymentRequest();
        req.setProvider("KASPI");

        mockMvc.perform(post("/api/orders/1/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void kaspiWebhook_validPayload_returns200() throws Exception {
        PaymentTransactionDto txDto = buildTx(PaymentProvider.KASPI, PaymentStatus.SUCCESS);
        when(paymentService.handleWebhook(eq(PaymentProvider.KASPI), eq("KASPI-001"), eq("SUCCESS")))
                .thenReturn(txDto);

        WebhookPayload payload = new WebhookPayload();
        payload.setTransactionId("KASPI-001");
        payload.setStatus("SUCCESS");

        mockMvc.perform(post("/api/payments/webhook/kaspi")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCESS"));
    }

    @Test
    void stripeWebhook_validPayload_returns200() throws Exception {
        PaymentTransactionDto txDto = buildTx(PaymentProvider.STRIPE, PaymentStatus.SUCCESS);
        when(paymentService.handleWebhook(eq(PaymentProvider.STRIPE), eq("pi_001"), eq("succeeded")))
                .thenReturn(txDto);

        WebhookPayload payload = new WebhookPayload();
        payload.setTransactionId("pi_001");
        payload.setStatus("succeeded");

        mockMvc.perform(post("/api/payments/webhook/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(payload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("STRIPE"));
    }

    @Test
    @WithMockUser(username = "user@test.com")
    void pay_missingProvider_returns400() throws Exception {
        PaymentRequest req = new PaymentRequest();
        // provider is null

        mockMvc.perform(post("/api/orders/1/pay")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
