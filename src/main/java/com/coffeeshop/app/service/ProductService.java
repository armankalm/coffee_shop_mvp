package com.coffeeshop.app.service;

import com.coffeeshop.app.domain.Product;
import com.coffeeshop.app.domain.RefProductCategory;
import com.coffeeshop.app.dto.product.ProductDto;
import com.coffeeshop.app.repository.ProductRepository;
import com.coffeeshop.app.repository.RefProductCategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final RefProductCategoryRepository refProductCategoryRepository;
    private final FileStorageService fileStorageService;

    public ProductService(ProductRepository productRepository,
                          RefProductCategoryRepository refProductCategoryRepository,
                          FileStorageService fileStorageService) {
        this.productRepository = productRepository;
        this.refProductCategoryRepository = refProductCategoryRepository;
        this.fileStorageService = fileStorageService;
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

    @Transactional
    public ProductDto updateImage(Long productId, MultipartFile file) throws IOException {
        Product product = productRepository.findByIdWithToppings(productId)
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + productId));
        String oldImagePath = product.getImagePath();
        String imagePath = fileStorageService.store(file);
        product.setImagePath(imagePath);
        productRepository.save(product);
        if (oldImagePath != null) {
            fileStorageService.deleteIfExists(oldImagePath);
        }
        return ProductDto.from(product);
    }
}
