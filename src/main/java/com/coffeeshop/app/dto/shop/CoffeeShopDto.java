package com.coffeeshop.app.dto.shop;

import com.coffeeshop.app.domain.CoffeeShop;
import com.coffeeshop.app.domain.ShopStatus;

public class CoffeeShopDto {
    private Long id;
    private String name;
    private String city;
    private String address;
    private ShopStatus status;

    public CoffeeShopDto() {}

    public static CoffeeShopDto from(CoffeeShop shop) {
        CoffeeShopDto dto = new CoffeeShopDto();
        dto.id = shop.getId();
        dto.name = shop.getName();
        dto.city = shop.getCity();
        dto.address = shop.getAddress();
        dto.status = shop.getStatus();
        return dto;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCity() { return city; }
    public String getAddress() { return address; }
    public ShopStatus getStatus() { return status; }
}
