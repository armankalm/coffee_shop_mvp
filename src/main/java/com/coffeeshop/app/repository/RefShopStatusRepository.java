package com.coffeeshop.app.repository;

import com.coffeeshop.app.domain.RefShopStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefShopStatusRepository extends JpaRepository<RefShopStatus, Long> {
    Optional<RefShopStatus> findByCode(String code);
}
