package com.coffeeshop.app.service;

import com.coffeeshop.app.domain.CoffeeShop;
import com.coffeeshop.app.domain.ShopStatus;
import com.coffeeshop.app.dto.shop.CoffeeShopDto;
import com.coffeeshop.app.repository.CoffeeShopRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CoffeeShopServiceTest {

    @Mock
    private CoffeeShopRepository coffeeShopRepository;

    @InjectMocks
    private CoffeeShopService coffeeShopService;

    private CoffeeShop buildShop(Long id, String name, String city) {
        return CoffeeShop.builder()
                .id(id).name(name).city(city)
                .address("123 Main St")
                .status(ShopStatus.OPEN)
                .build();
    }

    @Test
    void getAllGroupedByCity_groupsCorrectly() {
        CoffeeShop shop1 = buildShop(1L, "Downtown Cafe", "Almaty");
        CoffeeShop shop2 = buildShop(2L, "North Cafe", "Almaty");
        CoffeeShop shop3 = buildShop(3L, "Capital Cafe", "Astana");
        when(coffeeShopRepository.findAll()).thenReturn(List.of(shop1, shop2, shop3));

        Map<String, List<CoffeeShopDto>> result = coffeeShopService.getAllGroupedByCity();

        assertThat(result).containsKeys("Almaty", "Astana");
        assertThat(result.get("Almaty")).hasSize(2);
        assertThat(result.get("Astana")).hasSize(1);
    }

    @Test
    void getById_existingId_returnsDto() {
        CoffeeShop shop = buildShop(1L, "Test Cafe", "Almaty");
        when(coffeeShopRepository.findById(1L)).thenReturn(Optional.of(shop));

        CoffeeShopDto result = coffeeShopService.getById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Cafe");
        assertThat(result.getCity()).isEqualTo("Almaty");
    }

    @Test
    void getById_missingId_throwsNoSuchElement() {
        when(coffeeShopRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> coffeeShopService.getById(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    @Test
    void search_returnsMatchingShops() {
        CoffeeShop shop = buildShop(1L, "Downtown Cafe", "Almaty");
        when(coffeeShopRepository
                .findByNameContainingIgnoreCaseOrCityContainingIgnoreCaseOrAddressContainingIgnoreCase(
                        "downtown", "downtown", "downtown"))
                .thenReturn(List.of(shop));

        List<CoffeeShopDto> result = coffeeShopService.search("downtown");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Downtown Cafe");
    }
}
