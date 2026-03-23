package com.coffeeshop.app.dto.admin;

import jakarta.validation.constraints.NotBlank;

public class CreateShopRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String city;

    @NotBlank
    private String address;

    @NotBlank
    private String statusCode;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getStatusCode() { return statusCode; }
    public void setStatusCode(String statusCode) { this.statusCode = statusCode; }
}
