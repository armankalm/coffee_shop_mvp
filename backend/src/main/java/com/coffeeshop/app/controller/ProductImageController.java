package com.coffeeshop.app.controller;

import com.coffeeshop.app.dto.product.ProductDto;
import com.coffeeshop.app.service.ProductService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/products")
public class ProductImageController {

    private final ProductService productService;

    public ProductImageController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping("/{id}/image")
    @PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")
    public ResponseEntity<ProductDto> uploadImage(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(productService.updateImage(id, file));
    }
}
