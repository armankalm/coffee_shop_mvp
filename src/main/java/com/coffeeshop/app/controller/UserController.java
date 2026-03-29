package com.coffeeshop.app.controller;

import com.coffeeshop.app.dto.admin.UserDto;
import com.coffeeshop.app.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PatchMapping("/me/shop")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<UserDto> updateMyShop(
            Authentication authentication,
            @RequestBody Map<String, Long> body) {
        Long shopId = body.get("shopId");
        if (shopId == null) {
            throw new IllegalArgumentException("shopId is required");
        }
        UserDto updated = userService.updateUserShop(authentication.getName(), shopId);
        return ResponseEntity.ok(updated);
    }
}
