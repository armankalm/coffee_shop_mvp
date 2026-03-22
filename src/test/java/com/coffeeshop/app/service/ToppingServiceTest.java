package com.coffeeshop.app.service;

import com.coffeeshop.app.domain.Topping;
import com.coffeeshop.app.domain.ToppingType;
import com.coffeeshop.app.dto.product.ToppingDto;
import com.coffeeshop.app.repository.ToppingRepository;
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

    private Topping buildTopping(Long id, String name, ToppingType type) {
        return Topping.builder()
                .id(id).name(name).type(type)
                .price(BigDecimal.valueOf(100))
                .build();
    }

    @Test
    void getAllGroupedByType_groupsCorrectly() {
        Topping milk = buildTopping(1L, "Oat Milk", ToppingType.MILK);
        Topping syrup = buildTopping(2L, "Vanilla Syrup", ToppingType.SYRUP);
        Topping syrup2 = buildTopping(3L, "Caramel Syrup", ToppingType.SYRUP);
        when(toppingRepository.findAll()).thenReturn(List.of(milk, syrup, syrup2));

        Map<ToppingType, List<ToppingDto>> result = toppingService.getAllGroupedByType();

        assertThat(result).containsKeys(ToppingType.MILK, ToppingType.SYRUP);
        assertThat(result.get(ToppingType.MILK)).hasSize(1);
        assertThat(result.get(ToppingType.SYRUP)).hasSize(2);
    }

    @Test
    void validateCompatibility_noConflicts_passes() {
        Topping milk = buildTopping(1L, "Oat Milk", ToppingType.MILK);
        Topping syrup = buildTopping(2L, "Vanilla Syrup", ToppingType.SYRUP);
        when(toppingRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(milk, syrup));

        // Should not throw
        toppingService.validateCompatibility(Set.of(1L, 2L));
    }

    @Test
    void validateCompatibility_withConflict_throwsException() {
        Topping oatMilk = buildTopping(1L, "Oat Milk", ToppingType.MILK);
        Topping coconutMilk = buildTopping(2L, "Coconut Milk", ToppingType.MILK);
        // oatMilk is incompatible with coconutMilk
        oatMilk.getIncompatibleWith().add(coconutMilk);

        when(toppingRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(oatMilk, coconutMilk));

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
