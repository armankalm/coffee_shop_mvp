package com.coffeeshop.app.service;

import com.coffeeshop.app.domain.City;
import com.coffeeshop.app.domain.CoffeeShop;
import com.coffeeshop.app.dto.shop.CityDto;
import com.coffeeshop.app.dto.shop.CoffeeShopDto;
import com.coffeeshop.app.repository.CityRepository;
import com.coffeeshop.app.repository.CoffeeShopRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CityService {

    private final CityRepository cityRepository;
    private final CoffeeShopRepository coffeeShopRepository;

    public CityService(CityRepository cityRepository, CoffeeShopRepository coffeeShopRepository) {
        this.cityRepository = cityRepository;
        this.coffeeShopRepository = coffeeShopRepository;
    }

    public List<CityDto> getActiveCities() {
        return cityRepository.findByActiveTrue().stream()
                .map(CityDto::from)
                .collect(Collectors.toList());
    }

    public List<CoffeeShopDto> getShopsByCity(Long cityId) {
        City city = cityRepository.findById(cityId)
                .orElseThrow(() -> new NoSuchElementException("City not found: " + cityId));
        return coffeeShopRepository.findByCityWithDetails(city).stream()
                .map(CoffeeShopDto::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public CityDto createCity(String name, String region, String country) {
        City city = City.builder()
                .name(name)
                .region(region)
                .country(country)
                .active(true)
                .build();
        return CityDto.from(cityRepository.save(city));
    }

    @Transactional
    public CityDto updateCity(Long id, String name, String region, String country, boolean active) {
        City city = cityRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("City not found: " + id));
        city.setName(name);
        city.setRegion(region);
        city.setCountry(country);
        city.setActive(active);
        return CityDto.from(cityRepository.save(city));
    }

    @Transactional
    public void deleteCity(Long id) {
        if (!cityRepository.existsById(id)) {
            throw new NoSuchElementException("City not found: " + id);
        }
        cityRepository.deleteById(id);
    }
}
