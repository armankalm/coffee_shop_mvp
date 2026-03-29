package com.coffeeshop.app.dto.admin;

import com.coffeeshop.app.domain.User;

import java.time.Instant;

public class UserDto {

    private Long id;
    private String email;
    private String role;
    private Instant createdAt;
    private Long coffeeShopId;

    public static UserDto from(User user) {
        UserDto dto = new UserDto();
        dto.id = user.getId();
        dto.email = user.getEmail();
        dto.role = user.getRole().getCode();
        dto.createdAt = user.getCreatedAt();
        dto.coffeeShopId = user.getCoffeeShop() != null ? user.getCoffeeShop().getId() : null;
        return dto;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public Instant getCreatedAt() { return createdAt; }
    public Long getCoffeeShopId() { return coffeeShopId; }
}
