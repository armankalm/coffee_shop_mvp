package com.coffeeshop.app.repository;

import com.coffeeshop.app.domain.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CityRepository extends JpaRepository<City, Long> {
    List<City> findByActiveTrue();
    List<City> findByNameContainingIgnoreCase(String name);
}
