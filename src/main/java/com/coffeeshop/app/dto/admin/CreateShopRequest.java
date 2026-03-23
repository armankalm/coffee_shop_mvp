package com.coffeeshop.app.dto.admin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CreateShopRequest {

    @NotBlank
    private String name;

    @NotNull
    private Long cityId;

    @NotBlank
    private String address;

    @NotBlank
    private String statusCode;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Long getCityId() { return cityId; }
    public void setCityId(Long cityId) { this.cityId = cityId; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getStatusCode() { return statusCode; }
    public void setStatusCode(String statusCode) { this.statusCode = statusCode; }
}
