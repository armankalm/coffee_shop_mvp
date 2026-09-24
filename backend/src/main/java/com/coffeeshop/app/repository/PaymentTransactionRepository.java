package com.coffeeshop.app.repository;

import com.coffeeshop.app.domain.PaymentStatus;
import com.coffeeshop.app.domain.PaymentTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentTransactionRepository extends JpaRepository<PaymentTransaction, Long> {

    List<PaymentTransaction> findByOrderId(Long orderId);

    Optional<PaymentTransaction> findByExternalId(String externalId);

    Optional<PaymentTransaction> findByOrderIdAndStatus(Long orderId, PaymentStatus status);
}
