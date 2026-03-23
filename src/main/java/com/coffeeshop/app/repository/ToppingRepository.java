package com.coffeeshop.app.repository;

import com.coffeeshop.app.domain.RefToppingType;
import com.coffeeshop.app.domain.Topping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ToppingRepository extends JpaRepository<Topping, Long> {
    List<Topping> findByType(RefToppingType type);

    @Query("SELECT DISTINCT t FROM Topping t " +
           "LEFT JOIN FETCH t.incompatibleWith ic " +
           "LEFT JOIN FETCH ic.type " +
           "JOIN FETCH t.type")
    List<Topping> findAllWithIncompatibilities();

    @Query("SELECT DISTINCT t FROM Topping t " +
           "LEFT JOIN FETCH t.incompatibleWith ic " +
           "LEFT JOIN FETCH ic.type " +
           "JOIN FETCH t.type " +
           "WHERE t.id = :id")
    Optional<Topping> findByIdWithIncompatibilities(@Param("id") Long id);
}
