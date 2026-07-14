package com.coffeeshop.app.repository;

import com.coffeeshop.app.domain.FavoriteProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteProductRepository extends JpaRepository<FavoriteProduct, Long> {

    @Query("SELECT DISTINCT fp FROM FavoriteProduct fp " +
           "JOIN FETCH fp.product p " +
           "LEFT JOIN FETCH p.availableToppings t " +
           "LEFT JOIN FETCH t.type " +
           "LEFT JOIN FETCH t.incompatibleWith " +
           "JOIN FETCH p.category " +
           "WHERE fp.user.id = :userId")
    List<FavoriteProduct> findByUserId(@Param("userId") Long userId);

    Optional<FavoriteProduct> findByUserIdAndProductId(Long userId, Long productId);

    boolean existsByUserIdAndProductId(Long userId, Long productId);
}
