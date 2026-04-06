package com.coffeeshop.app.controller;

import com.coffeeshop.app.dto.reference.*;
import com.coffeeshop.app.service.ReferenceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/reference")
public class ReferenceController {

    private final ReferenceService referenceService;

    public ReferenceController(ReferenceService referenceService) {
        this.referenceService = referenceService;
    }

    // --- Order Statuses ---

    @GetMapping("/order-statuses")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<RefOrderStatusDto>> getAllOrderStatuses() {
        return ResponseEntity.ok(referenceService.getAllOrderStatuses());
    }

    @GetMapping("/order-statuses/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<RefOrderStatusDto> getOrderStatusById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(referenceService.getOrderStatusById(id));
    }

    @PostMapping("/order-statuses")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RefOrderStatusDto> createOrderStatus(@Valid @RequestBody RefOrderStatusRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(referenceService.createOrderStatus(request));
    }

    @PutMapping("/order-statuses/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RefOrderStatusDto> updateOrderStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody RefOrderStatusRequest request) {
        return ResponseEntity.ok(referenceService.updateOrderStatus(id, request));
    }

    @DeleteMapping("/order-statuses/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteOrderStatus(@PathVariable("id") Long id) {
        referenceService.deleteOrderStatus(id);
        return ResponseEntity.noContent().build();
    }

    // --- Shop Statuses ---

    @GetMapping("/shop-statuses")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<RefShopStatusDto>> getAllShopStatuses() {
        return ResponseEntity.ok(referenceService.getAllShopStatuses());
    }

    @GetMapping("/shop-statuses/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<RefShopStatusDto> getShopStatusById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(referenceService.getShopStatusById(id));
    }

    @PostMapping("/shop-statuses")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RefShopStatusDto> createShopStatus(@Valid @RequestBody RefShopStatusRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(referenceService.createShopStatus(request));
    }

    @PutMapping("/shop-statuses/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RefShopStatusDto> updateShopStatus(
            @PathVariable("id") Long id,
            @Valid @RequestBody RefShopStatusRequest request) {
        return ResponseEntity.ok(referenceService.updateShopStatus(id, request));
    }

    @DeleteMapping("/shop-statuses/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteShopStatus(@PathVariable("id") Long id) {
        referenceService.deleteShopStatus(id);
        return ResponseEntity.noContent().build();
    }

    // --- User Roles ---

    @GetMapping("/user-roles")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<RefUserRoleDto>> getAllUserRoles() {
        return ResponseEntity.ok(referenceService.getAllUserRoles());
    }

    @GetMapping("/user-roles/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<RefUserRoleDto> getUserRoleById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(referenceService.getUserRoleById(id));
    }

    @PostMapping("/user-roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RefUserRoleDto> createUserRole(@Valid @RequestBody RefUserRoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(referenceService.createUserRole(request));
    }

    @PutMapping("/user-roles/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RefUserRoleDto> updateUserRole(
            @PathVariable("id") Long id,
            @Valid @RequestBody RefUserRoleRequest request) {
        return ResponseEntity.ok(referenceService.updateUserRole(id, request));
    }

    @DeleteMapping("/user-roles/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteUserRole(@PathVariable("id") Long id) {
        referenceService.deleteUserRole(id);
        return ResponseEntity.noContent().build();
    }

    // --- Topping Types ---

    @GetMapping("/topping-types")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<RefToppingTypeDto>> getAllToppingTypes() {
        return ResponseEntity.ok(referenceService.getAllToppingTypes());
    }

    @GetMapping("/topping-types/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<RefToppingTypeDto> getToppingTypeById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(referenceService.getToppingTypeById(id));
    }

    @PostMapping("/topping-types")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RefToppingTypeDto> createToppingType(@Valid @RequestBody RefToppingTypeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(referenceService.createToppingType(request));
    }

    @PutMapping("/topping-types/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RefToppingTypeDto> updateToppingType(
            @PathVariable("id") Long id,
            @Valid @RequestBody RefToppingTypeRequest request) {
        return ResponseEntity.ok(referenceService.updateToppingType(id, request));
    }

    @DeleteMapping("/topping-types/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteToppingType(@PathVariable("id") Long id) {
        referenceService.deleteToppingType(id);
        return ResponseEntity.noContent().build();
    }

    // --- Product Categories ---

    @GetMapping("/product-categories")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<List<RefProductCategoryDto>> getAllProductCategories() {
        return ResponseEntity.ok(referenceService.getAllProductCategories());
    }

    @GetMapping("/product-categories/{id}")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<RefProductCategoryDto> getProductCategoryById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(referenceService.getProductCategoryById(id));
    }

    @PostMapping("/product-categories")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RefProductCategoryDto> createProductCategory(@Valid @RequestBody RefProductCategoryRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(referenceService.createProductCategory(request));
    }

    @PutMapping("/product-categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RefProductCategoryDto> updateProductCategory(
            @PathVariable("id") Long id,
            @Valid @RequestBody RefProductCategoryRequest request) {
        return ResponseEntity.ok(referenceService.updateProductCategory(id, request));
    }

    @DeleteMapping("/product-categories/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProductCategory(@PathVariable("id") Long id) {
        referenceService.deleteProductCategory(id);
        return ResponseEntity.noContent().build();
    }
}
