package com.coffeeshop.app.service;

import com.coffeeshop.app.domain.OtpCode;
import com.coffeeshop.app.repository.OtpCodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

@Service
public class OtpService {

    private static final Logger log = LoggerFactory.getLogger(OtpService.class);

    private final OtpCodeRepository otpCodeRepository;
    private final JavaMailSender mailSender;

    @Value("${app.otp.expiration-minutes:5}")
    private int expirationMinutes;

    @Value("${app.otp.length:6}")
    private int otpLength;

    @Value("${spring.mail.username:noreply@coffeeshop.local}")
    private String fromEmail;

    public OtpService(OtpCodeRepository otpCodeRepository, JavaMailSender mailSender) {
        this.otpCodeRepository = otpCodeRepository;
        this.mailSender = mailSender;
    }

    @Transactional
    public void generateAndSend(String email) {
        otpCodeRepository.deleteByEmailAndUsedTrue(email);

        String code = generateCode();

        OtpCode otpCode = new OtpCode();
        otpCode.setEmail(email);
        otpCode.setCode(code);
        otpCode.setExpiresAt(Instant.now().plus(expirationMinutes, ChronoUnit.MINUTES));
        otpCode.setUsed(false);
        otpCodeRepository.save(otpCode);

        sendEmail(email, code);
        log.info("OTP sent to: {}", email);
    }

    @Transactional
    public boolean verifyCode(String email, String code) {
        Optional<OtpCode> otpOpt = otpCodeRepository
                .findTopByEmailAndUsedFalseAndExpiresAtAfterOrderByIdDesc(email, Instant.now());

        if (otpOpt.isEmpty()) {
            log.debug("No valid OTP found for: {}", email);
            return false;
        }

        OtpCode otp = otpOpt.get();
        if (!otp.getCode().equals(code)) {
            log.debug("OTP code mismatch for: {}", email);
            return false;
        }

        otp.setUsed(true);
        otpCodeRepository.save(otp);
        return true;
    }

    private String generateCode() {
        SecureRandom random = new SecureRandom();
        int max = (int) Math.pow(10, otpLength);
        int num = random.nextInt(max);
        return String.format("%0" + otpLength + "d", num);
    }

    private void sendEmail(String to, String code) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Your Coffee Shop login code");
            message.setText("Your login code is: " + code + "\n\nThis code expires in " + expirationMinutes + " minutes.");
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send OTP email to {}: {}", to, e.getMessage());
            throw new RuntimeException("Failed to send OTP email", e);
        }
    }
}
