package com.coffeeshop.app.repository;

import com.coffeeshop.app.domain.Order;
import com.coffeeshop.app.domain.RefOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByUserId(Long userId);
    List<Order> findByStatus(RefOrderStatus status);
    List<Order> findByStatusAndShopId(RefOrderStatus status, Long shopId);
    List<Order> findByShopId(Long shopId);

    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.items i " +
           "LEFT JOIN FETCH i.toppings " +
           "JOIN FETCH o.user u " +
           "JOIN FETCH u.role " +
           "JOIN FETCH o.shop s " +
           "JOIN FETCH s.city " +
           "JOIN FETCH s.status " +
           "JOIN FETCH o.status " +
           "WHERE o.id = :id")
    Optional<Order> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT DISTINCT o FROM Order o " +
           "LEFT JOIN FETCH o.items i " +
           "LEFT JOIN FETCH i.toppings " +
           "JOIN FETCH o.user u " +
           "JOIN FETCH u.role " +
           "JOIN FETCH o.shop s " +
           "JOIN FETCH s.city " +
           "JOIN FETCH s.status " +
           "JOIN FETCH o.status " +
           "WHERE o.user.id = :userId " +
           "ORDER BY o.createdAt DESC")
    List<Order> findByUserIdWithDetailsOrderByCreatedAtDesc(@Param("userId") Long userId);
}
