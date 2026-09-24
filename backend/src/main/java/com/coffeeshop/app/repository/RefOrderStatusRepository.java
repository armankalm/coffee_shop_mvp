package com.coffeeshop.app.repository;

import com.coffeeshop.app.domain.RefOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefOrderStatusRepository extends JpaRepository<RefOrderStatus, Long> {
    Optional<RefOrderStatus> findByCode(String code);
}
