package com.coffeeshop.app.repository;

import com.coffeeshop.app.domain.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    Optional<OtpCode> findTopByEmailAndUsedFalseAndExpiresAtAfterOrderByIdDesc(
            String email, Instant now);

    void deleteByEmailAndUsedTrue(String email);
}
