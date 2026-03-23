package com.coffeeshop.app.repository;

import com.coffeeshop.app.domain.RefToppingType;
import com.coffeeshop.app.domain.Topping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ToppingRepository extends JpaRepository<Topping, Long> {
    List<Topping> findByType(RefToppingType type);
}
