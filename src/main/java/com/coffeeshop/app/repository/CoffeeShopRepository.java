package com.coffeeshop.app.repository;

import com.coffeeshop.app.domain.CoffeeShop;
import com.coffeeshop.app.domain.ShopStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CoffeeShopRepository extends JpaRepository<CoffeeShop, Long> {
    List<CoffeeShop> findByCity(String city);
    List<CoffeeShop> findByStatus(ShopStatus status);
    List<CoffeeShop> findByNameContainingIgnoreCaseOrCityContainingIgnoreCaseOrAddressContainingIgnoreCase(
            String name, String city, String address);
}
