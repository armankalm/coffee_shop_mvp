package com.coffeeshop.app.service;

import com.coffeeshop.app.domain.*;
import com.coffeeshop.app.dto.reference.*;
import com.coffeeshop.app.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReferenceService {

    private final RefOrderStatusRepository orderStatusRepo;
    private final RefShopStatusRepository shopStatusRepo;
    private final RefUserRoleRepository userRoleRepo;
    private final RefToppingTypeRepository toppingTypeRepo;
    private final RefProductCategoryRepository productCategoryRepo;

    public ReferenceService(RefOrderStatusRepository orderStatusRepo,
                            RefShopStatusRepository shopStatusRepo,
                            RefUserRoleRepository userRoleRepo,
                            RefToppingTypeRepository toppingTypeRepo,
                            RefProductCategoryRepository productCategoryRepo) {
        this.orderStatusRepo = orderStatusRepo;
        this.shopStatusRepo = shopStatusRepo;
        this.userRoleRepo = userRoleRepo;
        this.toppingTypeRepo = toppingTypeRepo;
        this.productCategoryRepo = productCategoryRepo;
    }

    // --- Order Statuses ---

    @Transactional(readOnly = true)
    public List<RefOrderStatusDto> getAllOrderStatuses() {
        return orderStatusRepo.findAll().stream()
                .map(RefOrderStatusDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RefOrderStatusDto getOrderStatusById(Long id) {
        return orderStatusRepo.findById(id)
                .map(RefOrderStatusDto::from)
                .orElseThrow(() -> new NoSuchElementException("RefOrderStatus not found: " + id));
    }

    public RefOrderStatusDto createOrderStatus(RefOrderStatusRequest request) {
        RefOrderStatus entity = RefOrderStatus.builder()
                .code(request.getCode())
                .nameRu(request.getNameRu())
                .nameEn(request.getNameEn())
                .description(request.getDescription())
                .build();
        return RefOrderStatusDto.from(orderStatusRepo.save(entity));
    }

    public RefOrderStatusDto updateOrderStatus(Long id, RefOrderStatusRequest request) {
        RefOrderStatus entity = orderStatusRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("RefOrderStatus not found: " + id));
        entity.setCode(request.getCode());
        entity.setNameRu(request.getNameRu());
        entity.setNameEn(request.getNameEn());
        entity.setDescription(request.getDescription());
        return RefOrderStatusDto.from(orderStatusRepo.save(entity));
    }

    public void deleteOrderStatus(Long id) {
        if (!orderStatusRepo.existsById(id)) {
            throw new NoSuchElementException("RefOrderStatus not found: " + id);
        }
        orderStatusRepo.deleteById(id);
    }

    // --- Shop Statuses ---

    @Transactional(readOnly = true)
    public List<RefShopStatusDto> getAllShopStatuses() {
        return shopStatusRepo.findAll().stream()
                .map(RefShopStatusDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RefShopStatusDto getShopStatusById(Long id) {
        return shopStatusRepo.findById(id)
                .map(RefShopStatusDto::from)
                .orElseThrow(() -> new NoSuchElementException("RefShopStatus not found: " + id));
    }

    public RefShopStatusDto createShopStatus(RefShopStatusRequest request) {
        RefShopStatus entity = RefShopStatus.builder()
                .code(request.getCode())
                .nameRu(request.getNameRu())
                .nameEn(request.getNameEn())
                .build();
        return RefShopStatusDto.from(shopStatusRepo.save(entity));
    }

    public RefShopStatusDto updateShopStatus(Long id, RefShopStatusRequest request) {
        RefShopStatus entity = shopStatusRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("RefShopStatus not found: " + id));
        entity.setCode(request.getCode());
        entity.setNameRu(request.getNameRu());
        entity.setNameEn(request.getNameEn());
        return RefShopStatusDto.from(shopStatusRepo.save(entity));
    }

    public void deleteShopStatus(Long id) {
        if (!shopStatusRepo.existsById(id)) {
            throw new NoSuchElementException("RefShopStatus not found: " + id);
        }
        shopStatusRepo.deleteById(id);
    }

    // --- User Roles ---

    @Transactional(readOnly = true)
    public List<RefUserRoleDto> getAllUserRoles() {
        return userRoleRepo.findAll().stream()
                .map(RefUserRoleDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RefUserRoleDto getUserRoleById(Long id) {
        return userRoleRepo.findById(id)
                .map(RefUserRoleDto::from)
                .orElseThrow(() -> new NoSuchElementException("RefUserRole not found: " + id));
    }

    public RefUserRoleDto createUserRole(RefUserRoleRequest request) {
        RefUserRole entity = RefUserRole.builder()
                .code(request.getCode())
                .nameRu(request.getNameRu())
                .nameEn(request.getNameEn())
                .permissions(request.getPermissions())
                .build();
        return RefUserRoleDto.from(userRoleRepo.save(entity));
    }

    public RefUserRoleDto updateUserRole(Long id, RefUserRoleRequest request) {
        RefUserRole entity = userRoleRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("RefUserRole not found: " + id));
        entity.setCode(request.getCode());
        entity.setNameRu(request.getNameRu());
        entity.setNameEn(request.getNameEn());
        entity.setPermissions(request.getPermissions());
        return RefUserRoleDto.from(userRoleRepo.save(entity));
    }

    public void deleteUserRole(Long id) {
        if (!userRoleRepo.existsById(id)) {
            throw new NoSuchElementException("RefUserRole not found: " + id);
        }
        userRoleRepo.deleteById(id);
    }

    // --- Topping Types ---

    @Transactional(readOnly = true)
    public List<RefToppingTypeDto> getAllToppingTypes() {
        return toppingTypeRepo.findAll().stream()
                .map(RefToppingTypeDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RefToppingTypeDto getToppingTypeById(Long id) {
        return toppingTypeRepo.findById(id)
                .map(RefToppingTypeDto::from)
                .orElseThrow(() -> new NoSuchElementException("RefToppingType not found: " + id));
    }

    public RefToppingTypeDto createToppingType(RefToppingTypeRequest request) {
        RefToppingType entity = RefToppingType.builder()
                .code(request.getCode())
                .nameRu(request.getNameRu())
                .nameEn(request.getNameEn())
                .build();
        return RefToppingTypeDto.from(toppingTypeRepo.save(entity));
    }

    public RefToppingTypeDto updateToppingType(Long id, RefToppingTypeRequest request) {
        RefToppingType entity = toppingTypeRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("RefToppingType not found: " + id));
        entity.setCode(request.getCode());
        entity.setNameRu(request.getNameRu());
        entity.setNameEn(request.getNameEn());
        return RefToppingTypeDto.from(toppingTypeRepo.save(entity));
    }

    public void deleteToppingType(Long id) {
        if (!toppingTypeRepo.existsById(id)) {
            throw new NoSuchElementException("RefToppingType not found: " + id);
        }
        toppingTypeRepo.deleteById(id);
    }

    // --- Product Categories ---

    @Transactional(readOnly = true)
    public List<RefProductCategoryDto> getAllProductCategories() {
        return productCategoryRepo.findAll().stream()
                .map(RefProductCategoryDto::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RefProductCategoryDto getProductCategoryById(Long id) {
        return productCategoryRepo.findById(id)
                .map(RefProductCategoryDto::from)
                .orElseThrow(() -> new NoSuchElementException("RefProductCategory not found: " + id));
    }

    public RefProductCategoryDto createProductCategory(RefProductCategoryRequest request) {
        RefProductCategory entity = RefProductCategory.builder()
                .code(request.getCode())
                .nameRu(request.getNameRu())
                .nameEn(request.getNameEn())
                .icon(request.getIcon())
                .build();
        return RefProductCategoryDto.from(productCategoryRepo.save(entity));
    }

    public RefProductCategoryDto updateProductCategory(Long id, RefProductCategoryRequest request) {
        RefProductCategory entity = productCategoryRepo.findById(id)
                .orElseThrow(() -> new NoSuchElementException("RefProductCategory not found: " + id));
        entity.setCode(request.getCode());
        entity.setNameRu(request.getNameRu());
        entity.setNameEn(request.getNameEn());
        entity.setIcon(request.getIcon());
        return RefProductCategoryDto.from(productCategoryRepo.save(entity));
    }

    public void deleteProductCategory(Long id) {
        if (!productCategoryRepo.existsById(id)) {
            throw new NoSuchElementException("RefProductCategory not found: " + id);
        }
        productCategoryRepo.deleteById(id);
    }
}
