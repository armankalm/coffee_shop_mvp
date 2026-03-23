package com.coffeeshop.app.service;

import com.coffeeshop.app.domain.RefToppingType;
import com.coffeeshop.app.domain.Topping;
import com.coffeeshop.app.dto.product.ToppingDto;
import com.coffeeshop.app.repository.ToppingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ToppingServiceTest {

    @Mock
    private ToppingRepository toppingRepository;

    @InjectMocks
    private ToppingService toppingService;

    private RefToppingType milkType;
    private RefToppingType syrupType;

    @BeforeEach
    void setUp() {
        milkType = RefToppingType.builder().id(1L).code("MILK").nameRu("Молоко").nameEn("Milk").build();
        syrupType = RefToppingType.builder().id(2L).code("SYRUP").nameRu("Сиропы").nameEn("Syrup").build();
    }

    private Topping buildTopping(Long id, String name, RefToppingType type) {
        return Topping.builder()
                .id(id).name(name).type(type)
                .price(BigDecimal.valueOf(100))
                .build();
    }

    @Test
    void getAllGroupedByType_groupsCorrectly() {
        Topping milk = buildTopping(1L, "Oat Milk", milkType);
        Topping syrup = buildTopping(2L, "Vanilla Syrup", syrupType);
        Topping syrup2 = buildTopping(3L, "Caramel Syrup", syrupType);
        when(toppingRepository.findAllWithIncompatibilities()).thenReturn(List.of(milk, syrup, syrup2));

        Map<String, List<ToppingDto>> result = toppingService.getAllGroupedByType();

        assertThat(result).containsKeys("MILK", "SYRUP");
        assertThat(result.get("MILK")).hasSize(1);
        assertThat(result.get("SYRUP")).hasSize(2);
    }

    @Test
    void validateCompatibility_noConflicts_passes() {
        Topping milk = buildTopping(1L, "Oat Milk", milkType);
        Topping syrup = buildTopping(2L, "Vanilla Syrup", syrupType);
        when(toppingRepository.findAllByIdWithIncompatibilities(Set.of(1L, 2L))).thenReturn(List.of(milk, syrup));

        // Should not throw
        toppingService.validateCompatibility(Set.of(1L, 2L));
    }

    @Test
    void validateCompatibility_withConflict_throwsException() {
        Topping oatMilk = buildTopping(1L, "Oat Milk", milkType);
        Topping coconutMilk = buildTopping(2L, "Coconut Milk", milkType);
        // oatMilk is incompatible with coconutMilk
        oatMilk.getIncompatibleWith().add(coconutMilk);

        when(toppingRepository.findAllByIdWithIncompatibilities(Set.of(1L, 2L))).thenReturn(List.of(oatMilk, coconutMilk));

        assertThatThrownBy(() -> toppingService.validateCompatibility(Set.of(1L, 2L)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Incompatible toppings");
    }

    @Test
    void validateCompatibility_singleTopping_passes() {
        // Single topping can never be incompatible with itself
        toppingService.validateCompatibility(Set.of(1L));
        verifyNoInteractions(toppingRepository);
    }

    @Test
    void validateCompatibility_emptySet_passes() {
        toppingService.validateCompatibility(Set.of());
        verifyNoInteractions(toppingRepository);
    }
}
