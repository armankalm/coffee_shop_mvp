package com.coffeeshop.app.service;

import com.coffeeshop.app.domain.*;
import com.coffeeshop.app.dto.reference.*;
import com.coffeeshop.app.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReferenceServiceTest {

    @Mock private RefOrderStatusRepository orderStatusRepo;
    @Mock private RefShopStatusRepository shopStatusRepo;
    @Mock private RefUserRoleRepository userRoleRepo;
    @Mock private RefToppingTypeRepository toppingTypeRepo;
    @Mock private RefProductCategoryRepository productCategoryRepo;

    @InjectMocks
    private ReferenceService referenceService;

    // --- Order Statuses ---

    @Test
    void getAllOrderStatuses_returnsAll() {
        RefOrderStatus entity = RefOrderStatus.builder().id(1L).code("NEW")
                .nameRu("Новый").nameEn("New").description("desc").build();
        when(orderStatusRepo.findAll()).thenReturn(List.of(entity));

        List<RefOrderStatusDto> result = referenceService.getAllOrderStatuses();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCode()).isEqualTo("NEW");
        assertThat(result.get(0).getNameRu()).isEqualTo("Новый");
    }

    @Test
    void getOrderStatusById_found_returnsDto() {
        RefOrderStatus entity = RefOrderStatus.builder().id(1L).code("NEW")
                .nameRu("Новый").nameEn("New").build();
        when(orderStatusRepo.findById(1L)).thenReturn(Optional.of(entity));

        RefOrderStatusDto result = referenceService.getOrderStatusById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getCode()).isEqualTo("NEW");
    }

    @Test
    void getOrderStatusById_notFound_throwsNoSuchElement() {
        when(orderStatusRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> referenceService.getOrderStatusById(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createOrderStatus_savesAndReturns() {
        RefOrderStatusRequest request = new RefOrderStatusRequest();
        request.setCode("PENDING");
        request.setNameRu("Ожидание");
        request.setNameEn("Pending");
        request.setDescription("Waiting");

        when(orderStatusRepo.save(any(RefOrderStatus.class))).thenAnswer(inv -> {
            RefOrderStatus e = inv.getArgument(0);
            e = RefOrderStatus.builder().id(10L).code(e.getCode())
                    .nameRu(e.getNameRu()).nameEn(e.getNameEn()).description(e.getDescription()).build();
            return e;
        });

        RefOrderStatusDto result = referenceService.createOrderStatus(request);

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getCode()).isEqualTo("PENDING");
    }

    @Test
    void updateOrderStatus_notFound_throwsNoSuchElement() {
        when(orderStatusRepo.findById(99L)).thenReturn(Optional.empty());

        RefOrderStatusRequest request = new RefOrderStatusRequest();
        request.setCode("X");
        request.setNameRu("X");
        request.setNameEn("X");

        assertThatThrownBy(() -> referenceService.updateOrderStatus(99L, request))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void deleteOrderStatus_existing_deletes() {
        when(orderStatusRepo.existsById(1L)).thenReturn(true);

        referenceService.deleteOrderStatus(1L);

        verify(orderStatusRepo).deleteById(1L);
    }

    @Test
    void deleteOrderStatus_notFound_throwsNoSuchElement() {
        when(orderStatusRepo.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> referenceService.deleteOrderStatus(99L))
                .isInstanceOf(NoSuchElementException.class);
    }

    // --- Shop Statuses ---

    @Test
    void getAllShopStatuses_returnsAll() {
        RefShopStatus entity = RefShopStatus.builder().id(1L).code("OPEN")
                .nameRu("Открыто").nameEn("Open").build();
        when(shopStatusRepo.findAll()).thenReturn(List.of(entity));

        List<RefShopStatusDto> result = referenceService.getAllShopStatuses();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCode()).isEqualTo("OPEN");
    }

    @Test
    void createShopStatus_savesAndReturns() {
        RefShopStatusRequest request = new RefShopStatusRequest();
        request.setCode("OPEN");
        request.setNameRu("Открыто");
        request.setNameEn("Open");

        when(shopStatusRepo.save(any(RefShopStatus.class))).thenAnswer(inv -> {
            RefShopStatus e = inv.getArgument(0);
            return RefShopStatus.builder().id(1L).code(e.getCode())
                    .nameRu(e.getNameRu()).nameEn(e.getNameEn()).build();
        });

        RefShopStatusDto result = referenceService.createShopStatus(request);

        assertThat(result.getCode()).isEqualTo("OPEN");
        assertThat(result.getNameEn()).isEqualTo("Open");
    }

    // --- User Roles ---

    @Test
    void getAllUserRoles_returnsAll() {
        RefUserRole entity = RefUserRole.builder().id(1L).code("USER")
                .nameRu("Пользователь").nameEn("User").permissions("orders:read").build();
        when(userRoleRepo.findAll()).thenReturn(List.of(entity));

        List<RefUserRoleDto> result = referenceService.getAllUserRoles();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCode()).isEqualTo("USER");
        assertThat(result.get(0).getPermissions()).isEqualTo("orders:read");
    }

    @Test
    void createUserRole_savesAndReturns() {
        RefUserRoleRequest request = new RefUserRoleRequest();
        request.setCode("ADMIN");
        request.setNameRu("Администратор");
        request.setNameEn("Admin");
        request.setPermissions("*");

        when(userRoleRepo.save(any(RefUserRole.class))).thenAnswer(inv -> {
            RefUserRole e = inv.getArgument(0);
            return RefUserRole.builder().id(4L).code(e.getCode())
                    .nameRu(e.getNameRu()).nameEn(e.getNameEn()).permissions(e.getPermissions()).build();
        });

        RefUserRoleDto result = referenceService.createUserRole(request);

        assertThat(result.getId()).isEqualTo(4L);
        assertThat(result.getCode()).isEqualTo("ADMIN");
    }

    // --- Topping Types ---

    @Test
    void getAllToppingTypes_returnsAll() {
        RefToppingType entity = RefToppingType.builder().id(1L).code("MILK")
                .nameRu("Молоко").nameEn("Milk").build();
        when(toppingTypeRepo.findAll()).thenReturn(List.of(entity));

        List<RefToppingTypeDto> result = referenceService.getAllToppingTypes();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCode()).isEqualTo("MILK");
    }

    @Test
    void updateToppingType_validRequest_updatesAndReturns() {
        RefToppingType entity = RefToppingType.builder().id(1L).code("MILK")
                .nameRu("Молоко").nameEn("Milk").build();
        when(toppingTypeRepo.findById(1L)).thenReturn(Optional.of(entity));
        when(toppingTypeRepo.save(any(RefToppingType.class))).thenAnswer(inv -> inv.getArgument(0));

        RefToppingTypeRequest request = new RefToppingTypeRequest();
        request.setCode("MILK");
        request.setNameRu("Молочный");
        request.setNameEn("Dairy");

        RefToppingTypeDto result = referenceService.updateToppingType(1L, request);

        assertThat(result.getNameRu()).isEqualTo("Молочный");
        assertThat(result.getNameEn()).isEqualTo("Dairy");
    }

    // --- Product Categories ---

    @Test
    void getAllProductCategories_returnsAll() {
        RefProductCategory entity = RefProductCategory.builder().id(1L).code("COFFEE")
                .nameRu("Кофе").nameEn("Coffee").icon("coffee").build();
        when(productCategoryRepo.findAll()).thenReturn(List.of(entity));

        List<RefProductCategoryDto> result = referenceService.getAllProductCategories();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCode()).isEqualTo("COFFEE");
        assertThat(result.get(0).getIcon()).isEqualTo("coffee");
    }

    @Test
    void createProductCategory_savesAndReturns() {
        RefProductCategoryRequest request = new RefProductCategoryRequest();
        request.setCode("COFFEE");
        request.setNameRu("Кофе");
        request.setNameEn("Coffee");
        request.setIcon("coffee");

        when(productCategoryRepo.save(any(RefProductCategory.class))).thenAnswer(inv -> {
            RefProductCategory e = inv.getArgument(0);
            return RefProductCategory.builder().id(1L).code(e.getCode())
                    .nameRu(e.getNameRu()).nameEn(e.getNameEn()).icon(e.getIcon()).build();
        });

        RefProductCategoryDto result = referenceService.createProductCategory(request);

        assertThat(result.getCode()).isEqualTo("COFFEE");
        assertThat(result.getIcon()).isEqualTo("coffee");
    }

    @Test
    void deleteProductCategory_notFound_throwsNoSuchElement() {
        when(productCategoryRepo.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> referenceService.deleteProductCategory(99L))
                .isInstanceOf(NoSuchElementException.class);
    }
}
