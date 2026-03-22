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
    void generateAndSend_savesOtpCodeAndSendsEmail() {
        ArgumentCaptor<OtpCode> otpCaptor = ArgumentCaptor.forClass(OtpCode.class);
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        otpService.generateAndSend("user@example.com");

        verify(otpCodeRepository).deleteByEmailAndUsedTrue("user@example.com");
        verify(otpCodeRepository).save(otpCaptor.capture());
        verify(mailSender).send(any(SimpleMailMessage.class));

        OtpCode saved = otpCaptor.getValue();
        assertThat(saved.getEmail()).isEqualTo("user@example.com");
        assertThat(saved.getCode()).hasSize(6);
        assertThat(saved.isUsed()).isFalse();
        assertThat(saved.getExpiresAt()).isAfter(Instant.now());
    }

    @Test
    void generateAndSend_cleansUpUsedCodesBeforeGenerating() {
        doNothing().when(mailSender).send(any(SimpleMailMessage.class));

        otpService.generateAndSend("user@example.com");

        // deleteByEmailAndUsedTrue must be called before save
        var inOrder = inOrder(otpCodeRepository);
        inOrder.verify(otpCodeRepository).deleteByEmailAndUsedTrue("user@example.com");
        inOrder.verify(otpCodeRepository).save(any(OtpCode.class));
    }

    @Test
    void generateAndSend_mailFailure_throwsRuntimeException() {
        doThrow(new RuntimeException("SMTP error")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> otpService.generateAndSend("user@example.com"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Failed to send OTP email");
    }

    @Test
    void verifyCode_validCode_returnsTrue() {
        OtpCode otpCode = new OtpCode();
        otpCode.setEmail("user@example.com");
        otpCode.setCode("123456");
        otpCode.setUsed(false);
        otpCode.setExpiresAt(Instant.now().plusSeconds(300));

        when(otpCodeRepository.findTopByEmailAndUsedFalseAndExpiresAtAfterOrderByIdDesc(
                eq("user@example.com"), any(Instant.class)))
                .thenReturn(Optional.of(otpCode));
        when(otpCodeRepository.save(any(OtpCode.class))).thenReturn(otpCode);

        boolean result = otpService.verifyCode("user@example.com", "123456");

        assertThat(result).isTrue();
        assertThat(otpCode.isUsed()).isTrue();
        verify(otpCodeRepository).save(otpCode);
    }

    @Test
    void verifyCode_wrongCode_returnsFalse() {
        OtpCode otpCode = new OtpCode();
        otpCode.setEmail("user@example.com");
        otpCode.setCode("123456");
        otpCode.setUsed(false);
        otpCode.setExpiresAt(Instant.now().plusSeconds(300));

        when(otpCodeRepository.findTopByEmailAndUsedFalseAndExpiresAtAfterOrderByIdDesc(
                eq("user@example.com"), any(Instant.class)))
                .thenReturn(Optional.of(otpCode));

        boolean result = otpService.verifyCode("user@example.com", "999999");

        assertThat(result).isFalse();
        verify(otpCodeRepository, never()).save(any());
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
