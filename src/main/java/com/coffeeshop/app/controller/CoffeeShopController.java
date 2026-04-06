package com.coffeeshop.app.controller;

import com.coffeeshop.app.dto.shop.CoffeeShopDto;
import com.coffeeshop.app.service.CoffeeShopService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shops")
public class CoffeeShopController {

    private final CoffeeShopService coffeeShopService;

    public CoffeeShopController(CoffeeShopService coffeeShopService) {
        this.coffeeShopService = coffeeShopService;
    }

    @GetMapping
    public ResponseEntity<Map<String, List<CoffeeShopDto>>> getAll() {
        return ResponseEntity.ok(coffeeShopService.getAllGroupedByCity());
    }

    @GetMapping("/{id}")
    public ResponseEntity<CoffeeShopDto> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(coffeeShopService.getById(id));
    }

    @GetMapping("/search")
    public ResponseEntity<List<CoffeeShopDto>> search(@RequestParam("query") String query) {
        return ResponseEntity.ok(coffeeShopService.search(query));
    }
}
