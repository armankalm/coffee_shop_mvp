package com.coffeeshop.app.repository;

import com.coffeeshop.app.domain.FavoriteItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteItemRepository extends JpaRepository<FavoriteItem, Long> {
    List<FavoriteItem> findByUserId(Long userId);
    Optional<FavoriteItem> findByUserIdAndSavedCombinationId(Long userId, Long savedCombinationId);
    boolean existsByUserIdAndSavedCombinationId(Long userId, Long savedCombinationId);
}
