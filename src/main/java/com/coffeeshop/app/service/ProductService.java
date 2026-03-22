package com.coffeeshop.app.service;

import com.coffeeshop.app.domain.Product;
import com.coffeeshop.app.domain.ProductCategory;
import com.coffeeshop.app.dto.product.ProductDto;
import com.coffeeshop.app.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    public List<ProductDto> getAll(ProductCategory category) {
        List<Product> products;
        if (category != null) {
            products = productRepository.findByCategoryAndAvailableTrue(category);
        } else {
            products = productRepository.findByAvailableTrue();
        }
        return products.stream().map(ProductDto::from).collect(Collectors.toList());
    }

    public ProductDto getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + id));
        return ProductDto.from(product);
    }
}
