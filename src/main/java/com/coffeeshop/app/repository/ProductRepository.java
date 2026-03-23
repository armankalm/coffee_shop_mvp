package com.coffeeshop.app.repository;

import com.coffeeshop.app.domain.Product;
import com.coffeeshop.app.domain.RefProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategory(RefProductCategory category);
    List<Product> findByAvailableTrue();
    List<Product> findByCategoryAndAvailableTrue(RefProductCategory category);
}
