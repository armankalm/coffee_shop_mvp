package com.coffeeshop.app.dto.shop;

import com.coffeeshop.app.domain.City;

public class CityDto {
    private Long id;
    private String name;
    private String region;

    public CityDto() {}

    public static CityDto from(City city) {
        CityDto dto = new CityDto();
        dto.id = city.getId();
        dto.name = city.getName();
        dto.region = city.getRegion();
        return dto;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getRegion() { return region; }
}
