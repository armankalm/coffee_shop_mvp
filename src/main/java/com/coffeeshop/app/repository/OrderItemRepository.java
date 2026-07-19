package com.coffeeshop.app.repository;

import com.coffeeshop.app.domain.OrderItem;
import com.coffeeshop.app.domain.RefOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {

    @Query("SELECT DISTINCT i FROM OrderItem i " +
           "JOIN FETCH i.order o " +
           "JOIN FETCH o.shop s " +
           "JOIN FETCH i.product p " +
           "JOIN FETCH i.status st " +
           "LEFT JOIN FETCH i.toppings t " +
           "LEFT JOIN FETCH t.type " +
           "WHERE s.id IN :shopIds " +
           "ORDER BY o.createdAt ASC")
    List<OrderItem> findByShopIdInWithDetails(@Param("shopIds") Collection<Long> shopIds);

    @Query("SELECT DISTINCT i FROM OrderItem i " +
           "JOIN FETCH i.order o " +
           "JOIN FETCH o.shop s " +
           "JOIN FETCH i.product p " +
           "JOIN FETCH i.status st " +
           "LEFT JOIN FETCH i.toppings t " +
           "LEFT JOIN FETCH t.type " +
           "WHERE i.status = :status AND s.id IN :shopIds " +
           "ORDER BY o.createdAt ASC")
    List<OrderItem> findByStatusAndShopIdInWithDetails(@Param("status") RefOrderStatus status,
                                                       @Param("shopIds") Collection<Long> shopIds);

    @Query("SELECT i FROM OrderItem i " +
           "JOIN FETCH i.order o " +
           "JOIN FETCH o.shop s " +
           "JOIN FETCH i.product p " +
           "JOIN FETCH i.status st " +
           "LEFT JOIN FETCH i.toppings t " +
           "LEFT JOIN FETCH t.type " +
           "WHERE i.id = :id")
    Optional<OrderItem> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT i FROM OrderItem i JOIN FETCH i.status WHERE i.order.id = :orderId")
    List<OrderItem> findByOrderIdWithStatus(@Param("orderId") Long orderId);
}
