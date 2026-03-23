package com.coffeeshop.app.service;

import com.coffeeshop.app.domain.City;
import com.coffeeshop.app.domain.CoffeeShop;
import com.coffeeshop.app.domain.RefShopStatus;
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

    private City buildCity(Long id, String name) {
        return City.builder().id(id).name(name).region("Region").country("KZ").active(true).build();
    }

    private CoffeeShop buildShop(Long id, String name, City city) {
        RefShopStatus openStatus = RefShopStatus.builder().id(1L).code("OPEN").nameRu("Открыто").nameEn("Open").build();
        return CoffeeShop.builder()
                .id(id).name(name).city(city)
                .address("123 Main St")
                .status(openStatus)
                .build();
    }

    @Test
    void getAllGroupedByCity_groupsCorrectly() {
        City almaty = buildCity(1L, "Almaty");
        City astana = buildCity(2L, "Astana");
        CoffeeShop shop1 = buildShop(1L, "Downtown Cafe", almaty);
        CoffeeShop shop2 = buildShop(2L, "North Cafe", almaty);
        CoffeeShop shop3 = buildShop(3L, "Capital Cafe", astana);
        when(coffeeShopRepository.findAllWithDetails()).thenReturn(List.of(shop1, shop2, shop3));

        Map<String, List<CoffeeShopDto>> result = coffeeShopService.getAllGroupedByCity();

        assertThat(result).containsKeys("Almaty", "Astana");
        assertThat(result.get("Almaty")).hasSize(2);
        assertThat(result.get("Astana")).hasSize(1);
    }

    @Test
    void getById_existingId_returnsDto() {
        City almaty = buildCity(1L, "Almaty");
        CoffeeShop shop = buildShop(1L, "Test Cafe", almaty);
        when(coffeeShopRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(shop));

        CoffeeShopDto result = coffeeShopService.getById(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Cafe");
        assertThat(result.getCity().getName()).isEqualTo("Almaty");
    }

    @Test
    void getById_missingId_throwsNoSuchElement() {
        when(coffeeShopRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> coffeeShopService.getById(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    @Test
    void search_returnsMatchingShops() {
        City almaty = buildCity(1L, "Almaty");
        CoffeeShop shop = buildShop(1L, "Downtown Cafe", almaty);
        when(coffeeShopRepository.searchByNameOrCityOrAddress("downtown"))
                .thenReturn(List.of(shop));

        List<CoffeeShopDto> result = coffeeShopService.search("downtown");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Downtown Cafe");
    }
}
