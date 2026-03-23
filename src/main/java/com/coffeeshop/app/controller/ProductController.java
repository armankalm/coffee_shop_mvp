package com.coffeeshop.app.controller;

import com.coffeeshop.app.dto.product.ProductDto;
import com.coffeeshop.app.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<List<ProductDto>> getAll(
            @RequestParam(required = false) String category) {
        return ResponseEntity.ok(productService.getAll(category));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }
}
