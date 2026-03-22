package com.coffeeshop.app.repository;

import com.coffeeshop.app.domain.Order;
import com.coffeeshop.app.domain.OrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
    List<Order> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Order> findByStatus(OrderStatus status);
    List<Order> findByStatusAndShopId(OrderStatus status, Long shopId);
    List<Order> findByShopId(Long shopId);
}
