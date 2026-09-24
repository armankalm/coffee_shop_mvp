package com.coffeeshop.app.service.push;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

import static com.coffeeshop.app.service.push.WebPushCrypto.base64UrlDecode;
import static com.coffeeshop.app.service.push.WebPushCrypto.base64UrlEncode;
import static org.assertj.core.api.Assertions.assertThat;

class WebPushCryptoTest {

    @Test
    void encrypt_roundTripsThroughTheReceiverKeys() throws Exception {
        KeyPair browser = WebPushCrypto.generateKeyPair();
        byte[] browserPublic = WebPushCrypto.encodePublicKey((ECPublicKey) browser.getPublic());
        byte[] authSecret = base64UrlDecode("BTBZMqHH6r4Tts7J_aSIgg");
        byte[] plaintext = "{\"title\":\"Заказ №7 готов\"}".getBytes(StandardCharsets.UTF_8);

        byte[] message = WebPushCrypto.encrypt(plaintext, browserPublic, authSecret);

        assertThat(WebPushCrypto.decrypt(message, (ECPrivateKey) browser.getPrivate(), browserPublic, authSecret))
                .isEqualTo(plaintext);
    }

    /** The worked example from RFC 8291 section 5. */
    @Test
    void encrypt_matchesRfc8291Example() throws Exception {
        ECPrivateKey asPrivate = WebPushCrypto.decodePrivateKey(base64UrlDecode("yfWPiYE-n46HLnH0KqZOF1fJJU3MYrct3AELtAQ-oRw"));
        ECPublicKey asPublic = WebPushCrypto.decodePublicKey(base64UrlDecode(
                "BP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A8"));
        byte[] uaPublic = base64UrlDecode(
                "BCVxsr7N_eNgVRqvHtD0zTZsEc6-VV-JvLexhqUzORcxaOzi6-AYWXvTBHm4bjyPjs7Vd8pZGH6SRpkNtoIAiw4");
        byte[] authSecret = base64UrlDecode("BTBZMqHH6r4Tts7J_aSIgg");
        byte[] salt = base64UrlDecode("DGv6ra1nlYgDCS1FRnbzlw");
        byte[] plaintext = "When I grow up, I want to be a watermelon".getBytes(StandardCharsets.US_ASCII);

        byte[] message = WebPushCrypto.encrypt(plaintext, uaPublic, authSecret, new KeyPair(asPublic, asPrivate), salt);

        assertThat(base64UrlEncode(message)).isEqualTo(
                "DGv6ra1nlYgDCS1FRnbzlwAAEABBBP4z9KsN6nGRTbVYI_c7VJSPQTBtkgcy27mlmlMoZIIgDll6e3vCYLocInmYWAmS6TlzAC8wEqKK6PBru3jl7A_yl95bQpu6cVPTpK4Mqgkf1CXztLVBSt2Ks3oZwbuwXPXLWyouBWLVWGNWQexSgSxsj_Qulcy4a-fN");
    }

    @Test
    void publicKey_encodingRoundTrips() throws Exception {
        KeyPair pair = WebPushCrypto.generateKeyPair();
        byte[] encoded = WebPushCrypto.encodePublicKey((ECPublicKey) pair.getPublic());

        assertThat(encoded).hasSize(65);
        assertThat(WebPushCrypto.decodePublicKey(encoded)).isEqualTo(pair.getPublic());
    }
}
