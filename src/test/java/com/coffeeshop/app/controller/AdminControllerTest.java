package com.coffeeshop.app.controller;

import com.coffeeshop.app.domain.*;
import com.coffeeshop.app.dto.admin.*;
import com.coffeeshop.app.dto.order.OrderDto;
import com.coffeeshop.app.dto.product.ProductDto;
import com.coffeeshop.app.dto.product.ToppingDto;
import com.coffeeshop.app.dto.shop.CoffeeShopDto;
import com.coffeeshop.app.service.AdminService;
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

@WebMvcTest(AdminController.class)
@Import({com.coffeeshop.app.config.SecurityConfig.class,
         com.coffeeshop.app.config.GlobalExceptionHandler.class,
         com.coffeeshop.app.security.JwtAuthFilter.class,
         com.coffeeshop.app.security.JwtTokenProvider.class,
         com.coffeeshop.app.security.JwtProperties.class})
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AdminService adminService;

    private OrderDto buildOrderDto(Long id, OrderStatus status) {
        User user = User.builder().id(1L).email("user@test.com").role(Role.USER).build();
        CoffeeShop shop = CoffeeShop.builder().id(1L).name("Test Shop").city("Almaty")
                .address("123 St").status(ShopStatus.OPEN).build();
        Order order = Order.builder().id(id).user(user).shop(shop)
                .status(status).total(BigDecimal.valueOf(500)).build();
        return OrderDto.from(order);
    }

    private CoffeeShopDto buildShopDto(Long id, String name) {
        CoffeeShop shop = CoffeeShop.builder().id(id).name(name).city("Almaty")
                .address("123 St").status(ShopStatus.OPEN).build();
        return CoffeeShopDto.from(shop);
    }

    private ProductDto buildProductDto(Long id, String name) {
        Product product = Product.builder().id(id).name(name).category(ProductCategory.COFFEE)
                .basePrice(BigDecimal.valueOf(500)).available(true).build();
        return ProductDto.from(product);
    }

    private ToppingDto buildToppingDto(Long id, String name) {
        Topping topping = Topping.builder().id(id).name(name).type(ToppingType.MILK)
                .price(BigDecimal.valueOf(100)).build();
        return ToppingDto.from(topping);
    }

    // ---- Orders ----

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void getAllOrders_asAdmin_returnsOrders() throws Exception {
        when(adminService.getAllOrders(null, null)).thenReturn(List.of(buildOrderDto(1L, OrderStatus.NEW)));

        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("NEW"));
    }

    @Test
    @WithMockUser(username = "barista@test.com", roles = "BARISTA")
    void getAllOrders_asBarista_returnsOrders() throws Exception {
        when(adminService.getAllOrders(null, null)).thenReturn(List.of(buildOrderDto(1L, OrderStatus.NEW)));

        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = "USER")
    void getAllOrders_asUser_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isForbidden());
    }

    @Test
    void getAllOrders_unauthenticated_returns401() throws Exception {
        mockMvc.perform(get("/api/admin/orders"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void getAllOrders_withStatusFilter_returnsFilteredOrders() throws Exception {
        when(adminService.getAllOrders(OrderStatus.NEW, null)).thenReturn(List.of(buildOrderDto(1L, OrderStatus.NEW)));

        mockMvc.perform(get("/api/admin/orders").param("status", "NEW"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("NEW"));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void getOrderById_existingOrder_returnsOrder() throws Exception {
        when(adminService.getOrderById(1L)).thenReturn(buildOrderDto(1L, OrderStatus.NEW));

        mockMvc.perform(get("/api/admin/orders/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void getOrderById_notFound_returns404() throws Exception {
        when(adminService.getOrderById(99L)).thenThrow(new NoSuchElementException("Order not found: 99"));

        mockMvc.perform(get("/api/admin/orders/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(username = "barista@test.com", roles = "BARISTA")
    void updateOrderStatus_asBarista_updatesStatus() throws Exception {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus(OrderStatus.IN_PROGRESS);

        when(adminService.updateOrderStatus(eq(1L), eq(OrderStatus.IN_PROGRESS)))
                .thenReturn(buildOrderDto(1L, OrderStatus.IN_PROGRESS));

        mockMvc.perform(patch("/api/admin/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("IN_PROGRESS"));
    }

    @Test
    @WithMockUser(username = "user@test.com", roles = "USER")
    void updateOrderStatus_asUser_returns403() throws Exception {
        UpdateOrderStatusRequest request = new UpdateOrderStatusRequest();
        request.setStatus(OrderStatus.IN_PROGRESS);

        mockMvc.perform(patch("/api/admin/orders/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    // ---- Shops ----

    @Test
    @WithMockUser(username = "manager@test.com", roles = "MANAGER")
    void createShop_asManager_returnsCreatedShop() throws Exception {
        CreateShopRequest request = new CreateShopRequest();
        request.setName("New Shop");
        request.setCity("Almaty");
        request.setAddress("123 St");
        request.setStatus(ShopStatus.OPEN);

        when(adminService.createShop(any(CreateShopRequest.class))).thenReturn(buildShopDto(2L, "New Shop"));

        mockMvc.perform(post("/api/admin/shops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("New Shop"));
    }

    @Test
    @WithMockUser(username = "barista@test.com", roles = "BARISTA")
    void createShop_asBarista_returns403() throws Exception {
        CreateShopRequest request = new CreateShopRequest();
        request.setName("New Shop");
        request.setCity("Almaty");
        request.setAddress("123 St");
        request.setStatus(ShopStatus.OPEN);

        mockMvc.perform(post("/api/admin/shops")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "manager@test.com", roles = "MANAGER")
    void updateShop_asManager_returnsUpdatedShop() throws Exception {
        CreateShopRequest request = new CreateShopRequest();
        request.setName("Updated Shop");
        request.setCity("Almaty");
        request.setAddress("456 Ave");
        request.setStatus(ShopStatus.OPEN);

        when(adminService.updateShop(eq(1L), any(CreateShopRequest.class))).thenReturn(buildShopDto(1L, "Updated Shop"));

        mockMvc.perform(put("/api/admin/shops/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Shop"));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void deleteShop_existingShop_returns204() throws Exception {
        doNothing().when(adminService).deleteShop(1L);

        mockMvc.perform(delete("/api/admin/shops/1"))
                .andExpect(status().isNoContent());
    }

    // ---- Products ----

    @Test
    @WithMockUser(username = "manager@test.com", roles = "MANAGER")
    void createProduct_asManager_returnsCreatedProduct() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Espresso");
        request.setCategory(ProductCategory.COFFEE);
        request.setBasePrice(BigDecimal.valueOf(300));
        request.setAvailable(true);

        when(adminService.createProduct(any(CreateProductRequest.class))).thenReturn(buildProductDto(2L, "Espresso"));

        mockMvc.perform(post("/api/admin/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Espresso"));
    }

    @Test
    @WithMockUser(username = "manager@test.com", roles = "MANAGER")
    void updateProduct_asManager_returnsUpdatedProduct() throws Exception {
        CreateProductRequest request = new CreateProductRequest();
        request.setName("Updated Latte");
        request.setCategory(ProductCategory.COFFEE);
        request.setBasePrice(BigDecimal.valueOf(550));
        request.setAvailable(true);

        when(adminService.updateProduct(eq(1L), any(CreateProductRequest.class))).thenReturn(buildProductDto(1L, "Updated Latte"));

        mockMvc.perform(put("/api/admin/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated Latte"));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void deleteProduct_existingProduct_returns204() throws Exception {
        doNothing().when(adminService).deleteProduct(1L);

        mockMvc.perform(delete("/api/admin/products/1"))
                .andExpect(status().isNoContent());
    }

    // ---- Toppings ----

    @Test
    @WithMockUser(username = "manager@test.com", roles = "MANAGER")
    void createTopping_asManager_returnsCreatedTopping() throws Exception {
        CreateToppingRequest request = new CreateToppingRequest();
        request.setName("Oat Milk");
        request.setType(ToppingType.MILK);
        request.setPrice(BigDecimal.valueOf(100));

        when(adminService.createTopping(any(CreateToppingRequest.class))).thenReturn(buildToppingDto(2L, "Oat Milk"));

        mockMvc.perform(post("/api/admin/toppings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Oat Milk"));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void deleteTopping_existingTopping_returns204() throws Exception {
        doNothing().when(adminService).deleteTopping(1L);

        mockMvc.perform(delete("/api/admin/toppings/1"))
                .andExpect(status().isNoContent());
    }

    // ---- Users ----

    @Test
    @WithMockUser(username = "manager@test.com", roles = "MANAGER")
    void getAllUsers_asManager_returnsUsers() throws Exception {
        User user = User.builder().id(1L).email("user@test.com").role(Role.USER).build();
        when(adminService.getAllUsers()).thenReturn(List.of(UserDto.from(user)));

        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("user@test.com"))
                .andExpect(jsonPath("$[0].role").value("USER"));
    }

    @Test
    @WithMockUser(username = "barista@test.com", roles = "BARISTA")
    void getAllUsers_asBarista_returns403() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isForbidden());
    }
}
