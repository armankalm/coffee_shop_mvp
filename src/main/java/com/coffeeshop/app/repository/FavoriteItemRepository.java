package com.coffeeshop.app.repository;

import com.coffeeshop.app.domain.FavoriteItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteItemRepository extends JpaRepository<FavoriteItem, Long> {
    @Query("SELECT DISTINCT fi FROM FavoriteItem fi " +
           "JOIN FETCH fi.savedCombination sc " +
           "JOIN FETCH sc.product p " +
           "JOIN FETCH p.category " +
           "LEFT JOIN FETCH sc.toppings t " +
           "LEFT JOIN FETCH t.type " +
           "LEFT JOIN FETCH t.incompatibleWith " +
           "WHERE fi.user.id = :userId")
    List<FavoriteItem> findByUserId(@Param("userId") Long userId);
    Optional<FavoriteItem> findByUserIdAndSavedCombinationId(Long userId, Long savedCombinationId);
    boolean existsByUserIdAndSavedCombinationId(Long userId, Long savedCombinationId);
}
