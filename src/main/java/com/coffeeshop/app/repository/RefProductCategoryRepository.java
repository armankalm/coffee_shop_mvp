package com.coffeeshop.app.repository;

import com.coffeeshop.app.domain.RefProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefProductCategoryRepository extends JpaRepository<RefProductCategory, Long> {
    Optional<RefProductCategory> findByCode(String code);
}
