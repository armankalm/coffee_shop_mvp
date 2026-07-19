package com.coffeeshop.app.repository;

import com.coffeeshop.app.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

    @Query("SELECT u FROM User u JOIN FETCH u.role LEFT JOIN FETCH u.coffeeShop WHERE u.email = :email")
    Optional<User> findByEmailWithRole(@Param("email") String email);

    @Query("SELECT u FROM User u JOIN FETCH u.role " +
           "LEFT JOIN FETCH u.coffeeShop cs " +
           "LEFT JOIN FETCH cs.city " +
           "LEFT JOIN FETCH cs.status " +
           "WHERE u.email = :email")
    Optional<User> findByEmailWithDetails(@Param("email") String email);

    @Query("SELECT u FROM User u JOIN FETCH u.role " +
           "LEFT JOIN FETCH u.assignedShops s " +
           "LEFT JOIN FETCH s.city " +
           "LEFT JOIN FETCH s.status " +
           "WHERE u.email = :email")
    Optional<User> findByEmailWithAssignedShops(@Param("email") String email);

    @Query("SELECT u FROM User u JOIN FETCH u.role " +
           "LEFT JOIN FETCH u.assignedShops s " +
           "LEFT JOIN FETCH s.city " +
           "LEFT JOIN FETCH s.status " +
           "WHERE u.id = :id")
    Optional<User> findByIdWithAssignedShops(@Param("id") Long id);

    @Query("SELECT u FROM User u JOIN FETCH u.role LEFT JOIN FETCH u.coffeeShop")
    List<User> findAllWithRole();
}
