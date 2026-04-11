package com.coffeeshop.app.dto.admin;

import com.coffeeshop.app.domain.User;
import com.coffeeshop.app.dto.shop.CoffeeShopDto;

import java.time.Instant;

public class UserDto {

    private Long id;
    private String email;
    private String role;
    private Instant createdAt;
    private Long coffeeShopId;
    private String coffeeShopName;
    private CoffeeShopDto coffeeShop;

    public static UserDto from(User user) {
        UserDto dto = new UserDto();
        dto.id = user.getId();
        dto.email = user.getEmail();
        dto.role = user.getRole().getCode();
        dto.createdAt = user.getCreatedAt();
        dto.coffeeShopId = user.getCoffeeShop() != null ? user.getCoffeeShop().getId() : null;
        dto.coffeeShopName = user.getCoffeeShop() != null ? user.getCoffeeShop().getName() : null;
        return dto;
    }

    public static UserDto fromWithDetails(User user) {
        UserDto dto = from(user);
        if (user.getCoffeeShop() != null) {
            dto.coffeeShop = CoffeeShopDto.from(user.getCoffeeShop());
        }
        return dto;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getRole() { return role; }
    public Instant getCreatedAt() { return createdAt; }
    public Long getCoffeeShopId() { return coffeeShopId; }
    public String getCoffeeShopName() { return coffeeShopName; }
    public CoffeeShopDto getCoffeeShop() { return coffeeShop; }
}
