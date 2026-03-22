package com.coffeeshop.app.service;

import com.coffeeshop.app.domain.OtpCode;
import com.coffeeshop.app.repository.OtpCodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock
    private OtpCodeRepository otpCodeRepository;

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private OtpService otpService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(otpService, "expirationMinutes", 5);
        ReflectionTestUtils.setField(otpService, "otpLength", 6);
        ReflectionTestUtils.setField(otpService, "fromEmail", "noreply@coffeeshop.local");
    }

    @Test
    void generateAndSend_savesHashedOtpCodeAndSendsEmail() {
        ArgumentCaptor<OtpCode> otpCaptor = ArgumentCaptor.forClass(OtpCode.class);
        when(otpCodeRepository.findTopByEmailAndUsedFalseAndExpiresAtAfterOrderByIdDesc(
                eq("user@example.com"), any(Instant.class))).thenReturn(Optional.empty());
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        otpService.generateAndSend("user@example.com");

        verify(otpCodeRepository).deleteExpiredOrInvalidByEmail(eq("user@example.com"), any(Instant.class), anyInt());
        verify(otpCodeRepository).save(otpCaptor.capture());
        verify(mailSender).send(any(SimpleMailMessage.class));

        OtpCode saved = otpCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("user@example.com");
        // Code is stored as BCrypt hash, not plaintext
        assertThat(saved.getCode()).startsWith("$2a$");
        assertThat(saved.isUsed()).isFalse();
        assertThat(saved.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void generateAndSend_activeOtpExists_skipsGeneration() {
        OtpCode activeOtp = new OtpCode();
        activeOtp.setEmail("user@example.com");
        activeOtp.setCode("$2a$10$hash");
        activeOtp.setUsed(false);
        activeOtp.setExpiresAt(Instant.now().plusSeconds(300));
        activeOtp.setFailedAttempts(0);

        when(otpCodeRepository.findTopByEmailAndUsedFalseAndExpiresAtAfterOrderByIdDesc(
                eq("user@example.com"), any(Instant.class))).thenReturn(Optional.of(activeOtp));

        otpService.generateAndSend("user@example.com");

        // No new OTP should be saved and no email sent
        verify(otpCodeRepository, never()).save(any(OtpCode.class));
        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void generateAndSend_exhaustedOtpExists_generatesNew() {
        OtpCode exhaustedOtp = new OtpCode();
        exhaustedOtp.setEmail("user@example.com");
        exhaustedOtp.setCode("$2a$10$hash");
        exhaustedOtp.setUsed(false);
        exhaustedOtp.setExpiresAt(Instant.now().plusSeconds(300));
        exhaustedOtp.setFailedAttempts(5); // MAX_OTP_ATTEMPTS reached

        when(otpCodeRepository.findTopByEmailAndUsedFalseAndExpiresAtAfterOrderByIdDesc(
                eq("user@example.com"), any(Instant.class))).thenReturn(Optional.of(exhaustedOtp));
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        otpService.generateAndSend("user@example.com");

        verify(otpCodeRepository).deleteExpiredOrInvalidByEmail(eq("user@example.com"), any(Instant.class), anyInt());
        verify(otpCodeRepository).save(any(OtpCode.class));
        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void generateAndSend_mailFailure_throwsRuntimeException() {
        when(otpCodeRepository.findTopByEmailAndUsedFalseAndExpiresAtAfterOrderByIdDesc(
                eq("user@example.com"), any(Instant.class))).thenReturn(Optional.empty());
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> otpService.generateAndSend("user@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send OTP email");
    }

    @Test
    void verifyCode_validCode_returnsTrue() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String plainCode = "123456";
        OtpCode otpCode = new OtpCode();
        otpCode.setEmail("user@example.com");
        otpCode.setCode(encoder.encode(plainCode));
        otpCode.setUsed(false);
        otpCode.setExpiresAt(Instant.now().plusSeconds(300));

        when(otpCodeRepository.findTopByEmailAndUsedFalseAndExpiresAtAfterOrderByIdDesc(
                eq("user@example.com"), any(Instant.class)))
                .thenReturn(Optional.of(otpCode));
        when(otpCodeRepository.save(any(OtpCode.class))).thenReturn(otpCode);

        boolean result = otpService.verifyCode("user@example.com", plainCode);

        assertThat(result).isTrue();
        assertThat(otpCode.isUsed()).isTrue();
        verify(otpCodeRepository).save(otpCode);
    }

    @Test
    void verifyCode_wrongCode_returnsFalse() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        OtpCode otpCode = new OtpCode();
        otpCode.setEmail("user@example.com");
        otpCode.setCode(encoder.encode("123456"));
        otpCode.setUsed(false);
        otpCode.setExpiresAt(Instant.now().plusSeconds(300));

        when(otpCodeRepository.findTopByEmailAndUsedFalseAndExpiresAtAfterOrderByIdDesc(
                eq("user@example.com"), any(Instant.class)))
                .thenReturn(Optional.of(otpCode));

        boolean result = otpService.verifyCode("user@example.com", "999999");

        assertThat(result).isFalse();
        assertThat(otpCode.getFailedAttempts()).isEqualTo(1);
        verify(otpCodeRepository).save(otpCode);
    }

    @Test
    void verifyCode_noActiveOtp_returnsFalse() {
        when(otpCodeRepository.findTopByEmailAndUsedFalseAndExpiresAtAfterOrderByIdDesc(
                eq("user@example.com"), any(Instant.class)))
                .thenReturn(Optional.empty());

        boolean result = otpService.verifyCode("user@example.com", "123456");

        assertThat(result).isFalse();
    }
}
