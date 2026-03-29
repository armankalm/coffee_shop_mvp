package com.coffeeshop.app.service;

import com.coffeeshop.app.domain.Product;
import com.coffeeshop.app.domain.RefProductCategory;
import com.coffeeshop.app.dto.product.ProductDto;
import com.coffeeshop.app.repository.ProductRepository;
import com.coffeeshop.app.repository.RefProductCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final RefProductCategoryRepository refProductCategoryRepository;

    public ProductService(ProductRepository productRepository,
                          RefProductCategoryRepository refProductCategoryRepository) {
        this.productRepository = productRepository;
        this.refProductCategoryRepository = refProductCategoryRepository;
    }

    public List<ProductDto> getAll(Long shopId, String categoryCode) {
        List<Product> products;
        if (categoryCode != null) {
            RefProductCategory category = refProductCategoryRepository.findByCode(categoryCode)
                    .orElseThrow(() -> new NoSuchElementException("Unknown category code: " + categoryCode));
            products = productRepository.findByCategoryAndAvailableTrueWithToppingsByShop(category, shopId);
        } else {
            products = productRepository.findAllAvailableWithToppingsByShop(shopId);
        }
        return products.stream().map(ProductDto::from).collect(Collectors.toList());
    }

    public ProductDto getById(Long id) {
        Product product = productRepository.findByIdWithToppings(id)
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + id));
        return ProductDto.from(product);
    }
}
