package com.coffeeshop.app.service;

import com.coffeeshop.app.domain.CoffeeShop;
import com.coffeeshop.app.domain.User;
import com.coffeeshop.app.dto.admin.UserDto;
import com.coffeeshop.app.repository.CoffeeShopRepository;
import com.coffeeshop.app.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.NoSuchElementException;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final CoffeeShopRepository coffeeShopRepository;

    public UserService(UserRepository userRepository, CoffeeShopRepository coffeeShopRepository) {
        this.userRepository = userRepository;
        this.coffeeShopRepository = coffeeShopRepository;
    }

    @Transactional
    public UserDto updateUserShop(String email, Long shopId) {
        User user = userRepository.findByEmailWithRole(email)
                .orElseThrow(() -> new NoSuchElementException("User not found: " + email));
        CoffeeShop shop = coffeeShopRepository.findById(shopId)
                .orElseThrow(() -> new NoSuchElementException("Coffee shop not found: " + shopId));
        user.setCoffeeShop(shop);
        userRepository.save(user);
        return UserDto.from(user);
    }
}
