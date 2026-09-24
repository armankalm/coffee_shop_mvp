package com.coffeeshop.app.service;

import com.coffeeshop.app.domain.City;
import com.coffeeshop.app.domain.CoffeeShop;
import com.coffeeshop.app.domain.RefShopStatus;
import com.coffeeshop.app.dto.shop.CityDto;
import com.coffeeshop.app.dto.shop.CoffeeShopDto;
import com.coffeeshop.app.repository.CityRepository;
import com.coffeeshop.app.repository.CoffeeShopRepository;
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
class CityServiceTest {

    @Mock
    private CityRepository cityRepository;

    @Mock
    private CoffeeShopRepository coffeeShopRepository;

    @InjectMocks
    private CityService cityService;

    private City buildCity(Long id, String name) {
        return City.builder().id(id).name(name).region("Region").country("KZ").active(true).build();
    }

    @Test
    void getActiveCities_returnsOnlyActiveCities() {
        City city1 = buildCity(1L, "Almaty");
        City city2 = buildCity(2L, "Astana");
        when(cityRepository.findByActiveTrue()).thenReturn(List.of(city1, city2));

        List<CityDto> result = cityService.getActiveCities();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(CityDto::getName).containsExactlyInAnyOrder("Almaty", "Astana");
    }

    @Test
    void getShopsByCity_existingCity_returnsShops() {
        City almaty = buildCity(1L, "Almaty");
        RefShopStatus status = RefShopStatus.builder().id(1L).code("OPEN").nameRu("Открыто").nameEn("Open").build();
        CoffeeShop shop = CoffeeShop.builder().id(1L).name("Cafe").city(almaty).address("Addr").status(status).build();
        when(cityRepository.findById(1L)).thenReturn(Optional.of(almaty));
        when(coffeeShopRepository.findByCityWithDetails(almaty)).thenReturn(List.of(shop));

        List<CoffeeShopDto> result = cityService.getShopsByCity(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Cafe");
    }

    @Test
    void getShopsByCity_missingCity_throwsNoSuchElement() {
        when(cityRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cityService.getShopsByCity(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createCity_savesAndReturnsDto() {
        City saved = buildCity(1L, "Almaty");
        when(cityRepository.save(any(City.class))).thenReturn(saved);

        CityDto result = cityService.createCity("Almaty", "Region", "KZ");

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Almaty");
    }

    @Test
    void updateCity_existingCity_updatesAndReturnsDto() {
        City city = buildCity(1L, "Almaty");
        when(cityRepository.findById(1L)).thenReturn(Optional.of(city));
        when(cityRepository.save(any(City.class))).thenAnswer(inv -> inv.getArgument(0));

        CityDto result = cityService.updateCity(1L, "Almaty Updated", "New Region", "KZ", true);

        assertThat(result.getName()).isEqualTo("Almaty Updated");
    }

    @Test
    void updateCity_missingCity_throwsNoSuchElement() {
        when(cityRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cityService.updateCity(99L, "X", null, null, true))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }

    @Test
    void deleteCity_existingCity_deletesSuccessfully() {
        when(cityRepository.existsById(1L)).thenReturn(true);

        cityService.deleteCity(1L);

        verify(cityRepository).deleteById(1L);
    }

    @Test
    void deleteCity_missingCity_throwsNoSuchElement() {
        when(cityRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> cityService.deleteCity(99L))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("99");
    }
}
