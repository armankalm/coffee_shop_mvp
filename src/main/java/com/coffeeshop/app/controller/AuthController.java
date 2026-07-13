package com.coffeeshop.app.controller;

import com.coffeeshop.app.dto.auth.AuthResponse;
import com.coffeeshop.app.dto.auth.RefreshTokenRequest;
import com.coffeeshop.app.dto.auth.RequestCodeRequest;
import com.coffeeshop.app.dto.auth.VerifyCodeRequest;
import com.coffeeshop.app.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/request-code")
    public ResponseEntity<Map<String, String>> requestCode(@Valid @RequestBody RequestCodeRequest request) {
        // In dev mode (no MAIL_USERNAME configured) devCode is returned instead of emailing the OTP.
        String devCode = authService.requestCode(request.getEmail());

        Map<String, String> body = new HashMap<>();
        body.put("message", "OTP sent to " + request.getEmail());
        if (devCode != null) {
            body.put("devCode", devCode);
        }
        return ResponseEntity.ok(body);
    }

    @PostMapping("/verify-code")
    public ResponseEntity<AuthResponse> verifyCode(@Valid @RequestBody VerifyCodeRequest request) {
        AuthResponse response = authService.verifyCode(request.getEmail(), request.getCode());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse response = authService.refresh(request.getRefreshToken());
        return ResponseEntity.ok(response);
    }
}
