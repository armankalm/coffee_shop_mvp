package com.coffeeshop.app.repository;

import com.coffeeshop.app.domain.SavedCombination;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SavedCombinationRepository extends JpaRepository<SavedCombination, Long> {
    @Query("SELECT DISTINCT sc FROM SavedCombination sc " +
           "JOIN FETCH sc.product p " +
           "JOIN FETCH p.category " +
           "LEFT JOIN FETCH sc.toppings t " +
           "LEFT JOIN FETCH t.type " +
           "LEFT JOIN FETCH t.incompatibleWith " +
           "WHERE sc.user.id = :userId")
    List<SavedCombination> findByUserId(@Param("userId") Long userId);
}
