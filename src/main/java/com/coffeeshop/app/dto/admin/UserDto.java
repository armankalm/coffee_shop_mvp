package com.coffeeshop.app.dto.admin;

import com.coffeeshop.app.domain.User;
import com.coffeeshop.app.dto.shop.CoffeeShopDto;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public class UserDto {

    private Long id;
    private String email;
    private String name;
    private String phone;
    private String role;
    private Instant createdAt;
    private Long coffeeShopId;
    private String coffeeShopName;
    private CoffeeShopDto coffeeShop;
    private List<CoffeeShopDto> assignedShops;

    public static UserDto from(User user) {
        UserDto dto = new UserDto();
        dto.id = user.getId();
        dto.email = user.getEmail();
        dto.name = user.getName();
        dto.phone = user.getPhone();
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

    public static UserDto fromWithAssignedShops(User user) {
        UserDto dto = from(user);
        dto.assignedShops = user.getAssignedShops().stream()
                .map(CoffeeShopDto::from)
                .collect(Collectors.toList());
        return dto;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getName() { return name; }
    public String getPhone() { return phone; }
    public String getRole() { return role; }
    public Instant getCreatedAt() { return createdAt; }
    public Long getCoffeeShopId() { return coffeeShopId; }
    public String getCoffeeShopName() { return coffeeShopName; }
    public CoffeeShopDto getCoffeeShop() { return coffeeShop; }
    public List<CoffeeShopDto> getAssignedShops() { return assignedShops; }
}
