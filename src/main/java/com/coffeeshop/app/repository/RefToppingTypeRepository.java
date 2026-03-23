package com.coffeeshop.app.repository;

import com.coffeeshop.app.domain.RefToppingType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefToppingTypeRepository extends JpaRepository<RefToppingType, Long> {
    Optional<RefToppingType> findByCode(String code);
}
