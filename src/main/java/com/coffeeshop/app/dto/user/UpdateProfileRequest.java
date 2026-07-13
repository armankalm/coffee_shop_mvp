package com.coffeeshop.app.dto.user;

import jakarta.validation.constraints.Size;

public class UpdateProfileRequest {

    @Size(max = 255)
    private String name;

    @Size(max = 32)
    private String phone;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
