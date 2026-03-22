package com.coffeeshop.app.dto.admin;

import com.coffeeshop.app.domain.Role;
import com.coffeeshop.app.domain.User;

import java.time.Instant;

public class UserDto {

    private Long id;
    private String email;
    private Role role;
    private Instant createdAt;

    public static UserDto from(User user) {
        UserDto dto = new UserDto();
        dto.id = user.getId();
        dto.email = user.getEmail();
        dto.role = user.getRole();
        dto.createdAt = user.getCreatedAt();
        return dto;
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public Role getRole() { return role; }
    public Instant getCreatedAt() { return createdAt; }
}
