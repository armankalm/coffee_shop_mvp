package com.coffeeshop.app.repository;

import com.coffeeshop.app.domain.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface OtpCodeRepository extends JpaRepository<OtpCode, Long> {

    Optional<OtpCode> findTopByEmailAndUsedFalseAndExpiresAtAfterOrderByIdDesc(
            String email, Instant now);

    @Modifying
    @Query("DELETE FROM OtpCode o WHERE o.email = :email AND (o.used = true OR o.expiresAt <= :now OR o.failedAttempts >= :maxAttempts)")
    void deleteExpiredOrInvalidByEmail(@Param("email") String email, @Param("now") Instant now, @Param("maxAttempts") int maxAttempts);

    void deleteByEmail(String email);
}
