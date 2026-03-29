package com.coffeeshop.app.controller;

import com.coffeeshop.app.dto.admin.UserDto;
import com.coffeeshop.app.dto.user.UpdateShopRequest;
import com.coffeeshop.app.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
            @Valid @RequestBody UpdateShopRequest body) {
        UserDto updated = userService.updateUserShop(authentication.getName(), body.getShopId());
        return ResponseEntity.ok(updated);
    }
}
