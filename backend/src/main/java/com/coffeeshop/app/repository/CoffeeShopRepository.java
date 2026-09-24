package com.coffeeshop.app.repository;

import com.coffeeshop.app.domain.City;
import com.coffeeshop.app.domain.CoffeeShop;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CoffeeShopRepository extends JpaRepository<CoffeeShop, Long> {
    @Query("SELECT DISTINCT s FROM CoffeeShop s " +
           "JOIN FETCH s.city " +
           "JOIN FETCH s.status " +
           "WHERE s.city = :city")
    List<CoffeeShop> findByCityWithDetails(@Param("city") City city);

    @Query("SELECT DISTINCT s FROM CoffeeShop s " +
           "JOIN FETCH s.city " +
           "JOIN FETCH s.status " +
           "WHERE LOWER(s.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.city.name) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(s.address) LIKE LOWER(CONCAT('%', :query, '%'))")
    List<CoffeeShop> searchByNameOrCityOrAddress(@Param("query") String query);

    @Query("SELECT DISTINCT s FROM CoffeeShop s " +
           "JOIN FETCH s.city " +
           "JOIN FETCH s.status")
    List<CoffeeShop> findAllWithDetails();

    @Query("SELECT DISTINCT s FROM CoffeeShop s " +
           "JOIN FETCH s.city " +
           "JOIN FETCH s.status " +
           "WHERE s.id = :id")
    Optional<CoffeeShop> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT s FROM CoffeeShop s " +
           "JOIN FETCH s.city " +
           "JOIN FETCH s.status " +
           "WHERE s.status.code = :statusCode " +
           "ORDER BY s.id ASC")
    List<CoffeeShop> findFirstByStatusCode(@Param("statusCode") String statusCode, Pageable pageable);
}
