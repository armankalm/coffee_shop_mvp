package com.coffeeshop.app.dto.shop;

import com.coffeeshop.app.domain.CoffeeShop;

public class CoffeeShopDto {
    private Long id;
    private String name;
    private CityDto city;
    private String address;
    private String status;
    private String statusNameRu;

    public CoffeeShopDto() {}

    public static CoffeeShopDto from(CoffeeShop shop) {
        CoffeeShopDto dto = new CoffeeShopDto();
        dto.id = shop.getId();
        dto.name = shop.getName();
        dto.city = CityDto.from(shop.getCity());
        dto.address = shop.getAddress();
        dto.status = shop.getStatus().getCode();
        dto.statusNameRu = shop.getStatus().getNameRu();
        return dto;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public CityDto getCity() { return city; }
    public String getAddress() { return address; }
    public String getStatus() { return status; }
    public String getStatusNameRu() { return statusNameRu; }
}
