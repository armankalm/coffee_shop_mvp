package com.coffeeshop.app.service.push;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WebPushSenderTest {

    @Test
    void vapidToken_isAnEs256JwtForThePushServiceOrigin() throws Exception {
        KeyPair keys = WebPushCrypto.generateKeyPair();
        WebPushSender sender = new WebPushSender(
                WebPushCrypto.base64UrlEncode(WebPushCrypto.encodePublicKey((ECPublicKey) keys.getPublic())),
                WebPushCrypto.base64UrlEncode(WebPushCrypto.encodePrivateKey((ECPrivateKey) keys.getPrivate())),
                "mailto:owner@example.com");

        String token = sender.vapidToken(URI.create("https://fcm.googleapis.com/fcm/send/abc"));
        String[] parts = token.split("\\.");
        String claims = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);

        assertThat(claims)
                .contains("\"aud\":\"https://fcm.googleapis.com\"")
                .contains("\"sub\":\"mailto:owner@example.com\"");
        Signature verifier = Signature.getInstance("SHA256withECDSAinP1363Format");
        verifier.initVerify(keys.getPublic());
        verifier.update((parts[0] + "." + parts[1]).getBytes(StandardCharsets.US_ASCII));
        assertThat(verifier.verify(Base64.getUrlDecoder().decode(parts[2]))).isTrue();
    }

    @Test
    void withoutKeys_pushIsDisabled() {
        assertThat(new WebPushSender("", "", "").isEnabled()).isFalse();
    }

    @Test
    void invalidKeys_failFast() {
        assertThatThrownBy(() -> new WebPushSender("not-a-key", "also-not", ""))
                .isInstanceOf(IllegalStateException.class);
    }
}
