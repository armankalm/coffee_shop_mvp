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

    /** Images are uploaded by the browser straight to Cloudinary; only its delivery URLs are accepted. */
    static final String CLOUDINARY_URL_PREFIX = "https://res.cloudinary.com/";

    /**
     * Stores the URL of an image the client already uploaded to Cloudinary
     * (folder {@code coffee-mvp/shop-<shopId>/products}). The old image is removed only when it is
     * kept by this server (local disk / Supabase); Cloudinary deletion needs the API secret.
     */
    @Transactional
    public ProductDto updateImageUrl(Long productId, String imageUrl) {
        if (imageUrl == null || !imageUrl.startsWith(CLOUDINARY_URL_PREFIX) || imageUrl.length() > 500) {
            throw new IllegalArgumentException("Image URL must be a Cloudinary delivery URL");
        }
        Product product = productRepository.findByIdWithToppings(productId)
                .orElseThrow(() -> new NoSuchElementException("Product not found: " + productId));
        String oldImagePath = product.getImagePath();
        product.setImagePath(imageUrl);
        productRepository.save(product);
        if (oldImagePath != null && !oldImagePath.startsWith(CLOUDINARY_URL_PREFIX)) {
            fileStorageService.deleteIfExists(oldImagePath);
        }
        return ProductDto.from(product);
    }
}
