package com.coffeeshop.app.controller;

import com.coffeeshop.app.dto.admin.*;
import com.coffeeshop.app.dto.order.OrderDto;
import com.coffeeshop.app.dto.order.OrderItemBoardDto;
import com.coffeeshop.app.dto.product.ProductDto;
import com.coffeeshop.app.dto.product.ToppingDto;
import com.coffeeshop.app.dto.shop.CityDto;
import com.coffeeshop.app.dto.shop.CoffeeShopDto;
import com.coffeeshop.app.service.AdminService;
import com.coffeeshop.app.service.CityService;
import com.coffeeshop.app.service.board.KitchenBoardSseService;
import com.coffeeshop.app.service.board.OrderBoardSseService;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasAnyRole('BARISTA', 'MANAGER', 'ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final CityService cityService;
    private final OrderBoardSseService orderBoardSseService;
    private final KitchenBoardSseService kitchenBoardSseService;

    public AdminController(AdminService adminService,
                           CityService cityService,
                           OrderBoardSseService orderBoardSseService,
                           KitchenBoardSseService kitchenBoardSseService) {
        this.adminService = adminService;
        this.cityService = cityService;
        this.orderBoardSseService = orderBoardSseService;
        this.kitchenBoardSseService = kitchenBoardSseService;
    }

    @GetMapping("/orders")
    public ResponseEntity<List<OrderDto>> getAllOrders(
            Authentication authentication,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "shopId", required = false) Long shopId) {
        return ResponseEntity.ok(adminService.getAllOrders(authentication.getName(), status, shopId));
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<OrderDto> getOrderById(
            Authentication authentication,
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(adminService.getOrderById(authentication.getName(), id));
    }

    @PatchMapping("/orders/{id}/status")
    public ResponseEntity<OrderDto> updateOrderStatus(
            Authentication authentication,
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateOrderStatusRequest request) {
        return ResponseEntity.ok(adminService.updateOrderStatus(authentication.getName(), id, request.getStatusCode()));
    }

    @GetMapping("/order-items")
    public ResponseEntity<List<OrderItemBoardDto>> getOrderItems(
            Authentication authentication,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "shopId", required = false) Long shopId) {
        return ResponseEntity.ok(adminService.getOrderItems(authentication.getName(), status, shopId));
    }

    @PatchMapping("/order-items/{id}/status")
    public ResponseEntity<OrderItemBoardDto> updateOrderItemStatus(
            Authentication authentication,
            @PathVariable("id") Long id,
            @Valid @RequestBody UpdateOrderItemStatusRequest request) {
        return ResponseEntity.ok(
                adminService.updateOrderItemStatus(authentication.getName(), id, request.getStatusCode()));
    }

    /**
     * Live pickup board (SSE). Emits a "board" event with the current
     * IN_PROGRESS + READY orders on subscribe and on every status change.
     * The access token is passed as a query parameter because the browser
     * EventSource API cannot send an Authorization header.
     */
    @GetMapping(value = "/orders/board/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamOrderBoard(
            Authentication authentication,
            @RequestParam(name = "shopId") Long shopId) {
        adminService.assertAssignedToShop(authentication.getName(), shopId);
        return orderBoardSseService.subscribe(shopId);
    }

    /**
     * Live kitchen board (SSE): streams the shop's order items and re-pushes the
     * full list on every status change, so staff see new POS/customer orders
     * appear without reloading.
     */
    @GetMapping(value = "/order-items/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamKitchenBoard(
            Authentication authentication,
            @RequestParam(name = "shopId") Long shopId) {
        adminService.assertAssignedToShop(authentication.getName(), shopId);
        return kitchenBoardSseService.subscribe(shopId);
    }

    @PostMapping("/shops")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<CoffeeShopDto> createShop(@Valid @RequestBody CreateShopRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createShop(request));
    }

    @PutMapping("/shops/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<CoffeeShopDto> updateShop(
            @PathVariable("id") Long id,
            @Valid @RequestBody CreateShopRequest request) {
        return ResponseEntity.ok(adminService.updateShop(id, request));
    }

    @DeleteMapping("/shops/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Void> deleteShop(@PathVariable("id") Long id) {
        adminService.deleteShop(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/products")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ProductDto> createProduct(@Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createProduct(request));
    }

    @PutMapping("/products/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ProductDto> updateProduct(
            @PathVariable("id") Long id,
            @Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity.ok(adminService.updateProduct(id, request));
    }

    @DeleteMapping("/products/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable("id") Long id) {
        adminService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/toppings")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ToppingDto> createTopping(@Valid @RequestBody CreateToppingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(adminService.createTopping(request));
    }

    @PutMapping("/toppings/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ToppingDto> updateTopping(
            @PathVariable("id") Long id,
            @Valid @RequestBody CreateToppingRequest request) {
        return ResponseEntity.ok(adminService.updateTopping(id, request));
    }

    @DeleteMapping("/toppings/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Void> deleteTopping(@PathVariable("id") Long id) {
        adminService.deleteTopping(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/users")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<UserDto>> getAllUsers() {
        return ResponseEntity.ok(adminService.getAllUsers());
    }

    @GetMapping("/users/{userId}/shops")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<CoffeeShopDto>> getAssignedShops(@PathVariable("userId") Long userId) {
        return ResponseEntity.ok(adminService.getAssignedShops(userId));
    }

    @PostMapping("/users/{userId}/shops/{shopId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<UserDto> assignShop(
            @PathVariable("userId") Long userId,
            @PathVariable("shopId") Long shopId) {
        return ResponseEntity.ok(adminService.assignShop(userId, shopId));
    }

    @DeleteMapping("/users/{userId}/shops/{shopId}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<UserDto> unassignShop(
            @PathVariable("userId") Long userId,
            @PathVariable("shopId") Long shopId) {
        return ResponseEntity.ok(adminService.unassignShop(userId, shopId));
    }

    @PostMapping("/orders/{id}/print")
    @PreAuthorize("hasAnyRole('BARISTA', 'MANAGER', 'ADMIN')")
    public ResponseEntity<Void> printOrder(@PathVariable("id") Long id) {
        adminService.printOrder(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/cities")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<CityDto>> getCities() {
        return ResponseEntity.ok(cityService.getAllCities());
    }

    @PostMapping("/cities")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<CityDto> createCity(@Valid @RequestBody CreateCityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cityService.createCity(request.getName(), request.getRegion(), request.getCountry()));
    }

    @PutMapping("/cities/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<CityDto> updateCity(
            @PathVariable("id") Long id,
            @Valid @RequestBody CreateCityRequest request) {
        return ResponseEntity.ok(cityService.updateCity(
                id, request.getName(), request.getRegion(), request.getCountry(), request.isActive()));
    }

    @DeleteMapping("/cities/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<Void> deleteCity(@PathVariable("id") Long id) {
        cityService.deleteCity(id);
        return ResponseEntity.noContent().build();
    }
}
