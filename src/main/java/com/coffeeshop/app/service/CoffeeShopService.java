package com.coffeeshop.app.service;

import com.coffeeshop.app.domain.CoffeeShop;
import com.coffeeshop.app.dto.shop.CoffeeShopDto;
import com.coffeeshop.app.repository.CoffeeShopRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CoffeeShopService {

    private final CoffeeShopRepository coffeeShopRepository;

    public CoffeeShopService(CoffeeShopRepository coffeeShopRepository) {
        this.coffeeShopRepository = coffeeShopRepository;
    }

    public Map<String, List<CoffeeShopDto>> getAllGroupedByCity() {
        return coffeeShopRepository.findAll().stream()
                .map(CoffeeShopDto::from)
                .collect(Collectors.groupingBy(CoffeeShopDto::getCity));
    }

    public CoffeeShopDto getById(Long id) {
        CoffeeShop shop = coffeeShopRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Coffee shop not found: " + id));
        return CoffeeShopDto.from(shop);
    }

    public List<CoffeeShopDto> search(String query) {
        return coffeeShopRepository
                .findByNameContainingIgnoreCaseOrCityContainingIgnoreCaseOrAddressContainingIgnoreCase(
                        query, query, query)
                .stream()
                .map(CoffeeShopDto::from)
                .collect(Collectors.toList());
    }
}
