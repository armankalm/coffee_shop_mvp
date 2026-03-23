package com.coffeeshop.app.controller;

import com.coffeeshop.app.dto.product.ToppingDto;
import com.coffeeshop.app.service.ToppingService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/toppings")
public class ToppingController {

    private final ToppingService toppingService;

    public ToppingController(ToppingService toppingService) {
        this.toppingService = toppingService;
    }

    @GetMapping
    public ResponseEntity<Map<String, List<ToppingDto>>> getAll() {
        return ResponseEntity.ok(toppingService.getAllGroupedByType());
    }
}
