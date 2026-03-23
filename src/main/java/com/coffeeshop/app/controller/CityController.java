package com.coffeeshop.app.controller;

import com.coffeeshop.app.dto.shop.CityDto;
import com.coffeeshop.app.dto.shop.CoffeeShopDto;
import com.coffeeshop.app.service.CityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cities")
public class CityController {

    private final CityService cityService;

    public CityController(CityService cityService) {
        this.cityService = cityService;
    }

    @GetMapping
    public ResponseEntity<List<CityDto>> getActiveCities() {
        return ResponseEntity.ok(cityService.getActiveCities());
    }

    @GetMapping("/{id}/shops")
    public ResponseEntity<List<CoffeeShopDto>> getShopsByCity(@PathVariable Long id) {
        return ResponseEntity.ok(cityService.getShopsByCity(id));
    }
}
