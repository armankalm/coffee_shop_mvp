package com.coffeeshop.app.repository;

import com.coffeeshop.app.domain.RefUserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefUserRoleRepository extends JpaRepository<RefUserRole, Long> {
    Optional<RefUserRole> findByCode(String code);
}
