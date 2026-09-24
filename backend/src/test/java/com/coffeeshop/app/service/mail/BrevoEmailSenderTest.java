package com.coffeeshop.app.service.mail;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BrevoEmailSenderTest {

    @Test
    void extractApiKey_bareKey_returnedAsIs() {
        assertThat(BrevoEmailSender.extractApiKey("xkeysib-abc-123")).isEqualTo("xkeysib-abc-123");
    }

    @Test
    void extractApiKey_symfonyDsn_returnsKeyOnly() {
        assertThat(BrevoEmailSender.extractApiKey("brevo+api://xkeysib-abc-123@default"))
                .isEqualTo("xkeysib-abc-123");
    }

    @Test
    void extractApiKey_blankOrNull_returnsEmpty() {
        assertThat(BrevoEmailSender.extractApiKey("  ")).isEmpty();
        assertThat(BrevoEmailSender.extractApiKey(null)).isEmpty();
    }
}
