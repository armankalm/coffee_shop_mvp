package com.coffeeshop.app.service;

import com.coffeeshop.app.domain.Role;
import com.coffeeshop.app.domain.User;
import com.coffeeshop.app.dto.auth.AuthResponse;
import com.coffeeshop.app.repository.UserRepository;
import com.coffeeshop.app.security.JwtTokenProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final OtpService otpService;
    private final JwtTokenProvider tokenProvider;

    public AuthService(UserRepository userRepository,
                       OtpService otpService,
                       JwtTokenProvider tokenProvider) {
        this.userRepository = userRepository;
        this.otpService = otpService;
        this.tokenProvider = tokenProvider;
    }

    @Transactional
    public void requestCode(String email) {
        // Auto-register new users; handle concurrent registration via the unique constraint
        if (!userRepository.existsByEmail(email)) {
            try {
                User user = User.builder()
                        .email(email)
                        .role(Role.USER)
                        .build();
                userRepository.saveAndFlush(user);
                log.info("Auto-registered new user: {}", email);
            } catch (DataIntegrityViolationException e) {
                // Another concurrent request already registered this user — that's fine
                log.debug("Concurrent registration for {}, user already exists", email);
            }
        }
        otpService.generateAndSend(email);
    }

    @Transactional
    public AuthResponse verifyCode(String email, String code) {
        boolean valid = otpService.verifyCode(email, code);
        if (!valid) {
            throw new IllegalArgumentException("Invalid or expired OTP code");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("User not found after OTP verification"));

        String role = user.getRole().name();
        String accessToken = tokenProvider.generateAccessToken(email, role);
        String refreshToken = tokenProvider.generateRefreshToken(email, role);
        return new AuthResponse(accessToken, refreshToken, email, role);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshToken) {
        if (!tokenProvider.validateToken(refreshToken) || !tokenProvider.isRefreshToken(refreshToken)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }

        String email = tokenProvider.getEmailFromToken(refreshToken);
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + email));

        String role = user.getRole().name();
        String newAccessToken = tokenProvider.generateAccessToken(email, role);
        String newRefreshToken = tokenProvider.generateRefreshToken(email, role);
        return new AuthResponse(newAccessToken, newRefreshToken, email, role);
    }
}
