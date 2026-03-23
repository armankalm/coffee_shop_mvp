package com.coffeeshop.app.dto.admin;

import jakarta.validation.constraints.NotBlank;

public class CreateCityRequest {

    @NotBlank
    private String name;

    private String region;

    private String country;

    private boolean active = true;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}
