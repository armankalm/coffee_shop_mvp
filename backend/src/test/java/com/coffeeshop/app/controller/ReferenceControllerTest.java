package com.coffeeshop.app.controller;

import com.coffeeshop.app.domain.*;
import com.coffeeshop.app.dto.reference.*;
import com.coffeeshop.app.service.ReferenceService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.NoSuchElementException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReferenceController.class)
@Import({com.coffeeshop.app.config.SecurityConfig.class,
         com.coffeeshop.app.config.GlobalExceptionHandler.class,
         com.coffeeshop.app.security.JwtAuthFilter.class,
         com.coffeeshop.app.security.JwtTokenProvider.class,
         com.coffeeshop.app.security.JwtProperties.class})
class ReferenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReferenceService referenceService;

    // --- Security checks ---

    @Test
    void getAllOrderStatuses_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/reference/order-statuses"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = "BARISTA")
    void getAllOrderStatuses_asBarista_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/reference/order-statuses"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "USER")
    void getAllOrderStatuses_asUser_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/reference/order-statuses"))
                .andExpect(status().isForbidden());
    }

    // --- Order Statuses ---

    @Test
    @WithMockUser(roles = "MANAGER")
    void getAllOrderStatuses_asManager_returnsOk() throws Exception {
        RefOrderStatusDto dto = RefOrderStatusDto.from(
                RefOrderStatus.builder().id(1L).code("NEW").nameRu("Новый").nameEn("New").description("d").build());
        when(referenceService.getAllOrderStatuses()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/admin/reference/order-statuses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("NEW"))
                .andExpect(jsonPath("$[0].nameRu").value("Новый"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getOrderStatusById_found_returnsDto() throws Exception {
        RefOrderStatusDto dto = RefOrderStatusDto.from(
                RefOrderStatus.builder().id(1L).code("NEW").nameRu("Новый").nameEn("New").build());
        when(referenceService.getOrderStatusById(1L)).thenReturn(dto);

        mockMvc.perform(get("/api/admin/reference/order-statuses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.code").value("NEW"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void getOrderStatusById_notFound_returns404() throws Exception {
        when(referenceService.getOrderStatusById(99L))
                .thenThrow(new NoSuchElementException("RefOrderStatus not found: 99"));

        mockMvc.perform(get("/api/admin/reference/order-statuses/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createOrderStatus_validRequest_returnsCreated() throws Exception {
        RefOrderStatusRequest request = new RefOrderStatusRequest();
        request.setCode("NEW");
        request.setNameRu("Новый");
        request.setNameEn("New");
        request.setDescription("desc");

        RefOrderStatusDto dto = RefOrderStatusDto.from(
                RefOrderStatus.builder().id(10L).code("NEW").nameRu("Новый").nameEn("New").description("desc").build());
        when(referenceService.createOrderStatus(any(RefOrderStatusRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/admin/reference/order-statuses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.code").value("NEW"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createOrderStatus_missingCode_returns400() throws Exception {
        RefOrderStatusRequest request = new RefOrderStatusRequest();
        // code is null - validation should fail
        request.setNameRu("Новый");
        request.setNameEn("New");

        mockMvc.perform(post("/api/admin/reference/order-statuses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void updateOrderStatus_asManager_returns403() throws Exception {
        RefOrderStatusRequest request = new RefOrderStatusRequest();
        request.setCode("NEW");
        request.setNameRu("Новый updated");
        request.setNameEn("New updated");

        mockMvc.perform(put("/api/admin/reference/order-statuses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateOrderStatus_asAdmin_returnsOk() throws Exception {
        RefOrderStatusRequest request = new RefOrderStatusRequest();
        request.setCode("NEW");
        request.setNameRu("Новый updated");
        request.setNameEn("New updated");

        RefOrderStatusDto dto = RefOrderStatusDto.from(
                RefOrderStatus.builder().id(1L).code("NEW").nameRu("Новый updated").nameEn("New updated").build());
        when(referenceService.updateOrderStatus(eq(1L), any(RefOrderStatusRequest.class))).thenReturn(dto);

        mockMvc.perform(put("/api/admin/reference/order-statuses/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nameRu").value("Новый updated"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteOrderStatus_existing_returnsNoContent() throws Exception {
        doNothing().when(referenceService).deleteOrderStatus(1L);

        mockMvc.perform(delete("/api/admin/reference/order-statuses/1"))
                .andExpect(status().isNoContent());
    }

    // --- Shop Statuses ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllShopStatuses_returnsOk() throws Exception {
        RefShopStatusDto dto = RefShopStatusDto.from(
                RefShopStatus.builder().id(1L).code("OPEN").nameRu("Открыто").nameEn("Open").build());
        when(referenceService.getAllShopStatuses()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/admin/reference/shop-statuses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("OPEN"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createShopStatus_validRequest_returnsCreated() throws Exception {
        RefShopStatusRequest request = new RefShopStatusRequest();
        request.setCode("OPEN");
        request.setNameRu("Открыто");
        request.setNameEn("Open");

        RefShopStatusDto dto = RefShopStatusDto.from(
                RefShopStatus.builder().id(1L).code("OPEN").nameRu("Открыто").nameEn("Open").build());
        when(referenceService.createShopStatus(any(RefShopStatusRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/admin/reference/shop-statuses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("OPEN"));
    }

    // --- User Roles ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllUserRoles_returnsOk() throws Exception {
        RefUserRoleDto dto = RefUserRoleDto.from(
                RefUserRole.builder().id(1L).code("USER").nameRu("Пользователь").nameEn("User")
                        .permissions("orders:read").build());
        when(referenceService.getAllUserRoles()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/admin/reference/user-roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("USER"))
                .andExpect(jsonPath("$[0].permissions").value("orders:read"));
    }

    // --- Topping Types ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllToppingTypes_returnsOk() throws Exception {
        RefToppingTypeDto dto = RefToppingTypeDto.from(
                RefToppingType.builder().id(1L).code("MILK").nameRu("Молоко").nameEn("Milk").build());
        when(referenceService.getAllToppingTypes()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/admin/reference/topping-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("MILK"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteToppingType_existing_returnsNoContent() throws Exception {
        doNothing().when(referenceService).deleteToppingType(1L);

        mockMvc.perform(delete("/api/admin/reference/topping-types/1"))
                .andExpect(status().isNoContent());
    }

    // --- Product Categories ---

    @Test
    @WithMockUser(roles = "ADMIN")
    void getAllProductCategories_returnsOk() throws Exception {
        RefProductCategoryDto dto = RefProductCategoryDto.from(
                RefProductCategory.builder().id(1L).code("COFFEE").nameRu("Кофе").nameEn("Coffee").icon("coffee").build());
        when(referenceService.getAllProductCategories()).thenReturn(List.of(dto));

        mockMvc.perform(get("/api/admin/reference/product-categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("COFFEE"))
                .andExpect(jsonPath("$[0].icon").value("coffee"));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void createProductCategory_asManager_returns403() throws Exception {
        RefProductCategoryRequest request = new RefProductCategoryRequest();
        request.setCode("COFFEE");
        request.setNameRu("Кофе");
        request.setNameEn("Coffee");
        request.setIcon("coffee");

        mockMvc.perform(post("/api/admin/reference/product-categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createProductCategory_asAdmin_returnsCreated() throws Exception {
        RefProductCategoryRequest request = new RefProductCategoryRequest();
        request.setCode("COFFEE");
        request.setNameRu("Кофе");
        request.setNameEn("Coffee");
        request.setIcon("coffee");

        RefProductCategoryDto dto = RefProductCategoryDto.from(
                RefProductCategory.builder().id(1L).code("COFFEE").nameRu("Кофе").nameEn("Coffee").icon("coffee").build());
        when(referenceService.createProductCategory(any(RefProductCategoryRequest.class))).thenReturn(dto);

        mockMvc.perform(post("/api/admin/reference/product-categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("COFFEE"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteProductCategory_notFound_returns404() throws Exception {
        doThrow(new NoSuchElementException("RefProductCategory not found: 99"))
                .when(referenceService).deleteProductCategory(99L);

        mockMvc.perform(delete("/api/admin/reference/product-categories/99"))
                .andExpect(status().isNotFound());
    }
}
