package com.coffeeshop.app.repository;

import com.coffeeshop.app.domain.Product;
import com.coffeeshop.app.domain.RefProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findByCategory(RefProductCategory category);
    List<Product> findByAvailableTrue();
    List<Product> findByCategoryAndAvailableTrue(RefProductCategory category);

    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.availableToppings t " +
           "LEFT JOIN FETCH t.type " +
           "JOIN FETCH p.category " +
           "WHERE p.available = true")
    List<Product> findAllAvailableWithToppings();

    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.availableToppings t " +
           "LEFT JOIN FETCH t.type " +
           "JOIN FETCH p.category " +
           "WHERE p.available = true AND p.category = :category")
    List<Product> findByCategoryAndAvailableTrueWithToppings(@Param("category") RefProductCategory category);

    @Query("SELECT DISTINCT p FROM Product p " +
           "LEFT JOIN FETCH p.availableToppings t " +
           "LEFT JOIN FETCH t.type " +
           "JOIN FETCH p.category " +
           "WHERE p.id = :id")
    Optional<Product> findByIdWithToppings(@Param("id") Long id);
}
