package com.coffeeshop.app.service.push;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.SecureRandom;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
import java.security.spec.ECPoint;
import java.security.spec.ECPrivateKeySpec;
import java.security.spec.ECPublicKeySpec;
import java.util.Arrays;
import java.util.Base64;

/**
 * Web Push payload encryption (RFC 8291, "aes128gcm" content coding from RFC 8188) and
 * the P-256 key handling it and VAPID (RFC 8292) need. Plain JDK crypto, no extra libraries.
 */
final class WebPushCrypto {

    static final int RECORD_SIZE = 4096;
    private static final int SALT_LENGTH = 16;
    private static final int TAG_BITS = 128;
    private static final byte PADDING_DELIMITER = 0x02; // last (and only) record
    private static final ECParameterSpec P256 = p256Parameters();
    private static final SecureRandom RANDOM = new SecureRandom();

    private WebPushCrypto() {
    }

    /** Encrypts with a fresh ephemeral key pair and salt, as every push message must. */
    static byte[] encrypt(byte[] plaintext, byte[] uaPublic, byte[] authSecret) throws GeneralSecurityException {
        byte[] salt = new byte[SALT_LENGTH];
        RANDOM.nextBytes(salt);
        return encrypt(plaintext, uaPublic, authSecret, generateKeyPair(), salt);
    }

    /** Deterministic variant for tests: the caller supplies the sender key pair and salt. */
    static byte[] encrypt(byte[] plaintext, byte[] uaPublic, byte[] authSecret, KeyPair senderKeys, byte[] salt)
            throws GeneralSecurityException {
        byte[] asPublic = encodePublicKey((ECPublicKey) senderKeys.getPublic());
        byte[] ecdhSecret = ecdh((ECPrivateKey) senderKeys.getPrivate(), decodePublicKey(uaPublic));
        byte[][] keyAndNonce = deriveKeyAndNonce(ecdhSecret, authSecret, uaPublic, asPublic, salt);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(keyAndNonce[0], "AES"),
                new GCMParameterSpec(TAG_BITS, keyAndNonce[1]));
        byte[] ciphertext = cipher.doFinal(concat(plaintext, new byte[]{PADDING_DELIMITER}));

        return ByteBuffer.allocate(SALT_LENGTH + 4 + 1 + asPublic.length + ciphertext.length)
                .put(salt)
                .putInt(RECORD_SIZE)
                .put((byte) asPublic.length)
                .put(asPublic)
                .put(ciphertext)
                .array();
    }

    /** Receiver side, used by tests to prove {@link #encrypt} round-trips. */
    static byte[] decrypt(byte[] message, ECPrivateKey uaPrivate, byte[] uaPublic, byte[] authSecret)
            throws GeneralSecurityException {
        ByteBuffer buffer = ByteBuffer.wrap(message);
        byte[] salt = new byte[SALT_LENGTH];
        buffer.get(salt);
        buffer.getInt(); // record size
        byte[] asPublic = new byte[buffer.get() & 0xff];
        buffer.get(asPublic);
        byte[] ciphertext = new byte[buffer.remaining()];
        buffer.get(ciphertext);

        byte[] ecdhSecret = ecdh(uaPrivate, decodePublicKey(asPublic));
        byte[][] keyAndNonce = deriveKeyAndNonce(ecdhSecret, authSecret, uaPublic, asPublic, salt);

        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(keyAndNonce[0], "AES"),
                new GCMParameterSpec(TAG_BITS, keyAndNonce[1]));
        byte[] padded = cipher.doFinal(ciphertext);
        int end = padded.length - 1;
        while (end >= 0 && padded[end] == 0) {
            end--;
        }
        if (end < 0 || padded[end] != PADDING_DELIMITER) {
            throw new GeneralSecurityException("Missing padding delimiter");
        }
        return Arrays.copyOf(padded, end);
    }

    /** RFC 8291 section 3.4: content-encryption key (16 bytes) and nonce (12 bytes). */
    private static byte[][] deriveKeyAndNonce(byte[] ecdhSecret, byte[] authSecret, byte[] uaPublic,
                                              byte[] asPublic, byte[] salt) throws GeneralSecurityException {
        byte[] prkKey = hmacSha256(authSecret, ecdhSecret);
        byte[] keyInfo = concat("WebPush: info\0".getBytes(StandardCharsets.US_ASCII), uaPublic, asPublic);
        byte[] ikm = hkdfExpand(prkKey, keyInfo, 32);

        byte[] prk = hmacSha256(salt, ikm);
        byte[] cek = hkdfExpand(prk, "Content-Encoding: aes128gcm\0".getBytes(StandardCharsets.US_ASCII), 16);
        byte[] nonce = hkdfExpand(prk, "Content-Encoding: nonce\0".getBytes(StandardCharsets.US_ASCII), 12);
        return new byte[][]{cek, nonce};
    }

    /** HKDF-Expand for output of at most one hash block (32 bytes), all RFC 8291 needs. */
    private static byte[] hkdfExpand(byte[] prk, byte[] info, int length) throws GeneralSecurityException {
        return Arrays.copyOf(hmacSha256(prk, concat(info, new byte[]{0x01})), length);
    }

    private static byte[] hmacSha256(byte[] key, byte[] data) throws GeneralSecurityException {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(key, "HmacSHA256"));
        return mac.doFinal(data);
    }

    private static byte[] ecdh(ECPrivateKey privateKey, ECPublicKey publicKey) throws GeneralSecurityException {
        KeyAgreement agreement = KeyAgreement.getInstance("ECDH");
        agreement.init(privateKey);
        agreement.doPhase(publicKey, true);
        return agreement.generateSecret();
    }

    static KeyPair generateKeyPair() throws GeneralSecurityException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("EC");
        generator.initialize(new ECGenParameterSpec("secp256r1"));
        return generator.generateKeyPair();
    }

    /** Parses an uncompressed P-256 point (0x04 || X || Y, 65 bytes). */
    static ECPublicKey decodePublicKey(byte[] point) throws GeneralSecurityException {
        if (point.length != 65 || point[0] != 0x04) {
            throw new GeneralSecurityException("Expected an uncompressed P-256 public key");
        }
        BigInteger x = new BigInteger(1, Arrays.copyOfRange(point, 1, 33));
        BigInteger y = new BigInteger(1, Arrays.copyOfRange(point, 33, 65));
        return (ECPublicKey) KeyFactory.getInstance("EC")
                .generatePublic(new ECPublicKeySpec(new ECPoint(x, y), P256));
    }

    static ECPrivateKey decodePrivateKey(byte[] scalar) throws GeneralSecurityException {
        return (ECPrivateKey) KeyFactory.getInstance("EC")
                .generatePrivate(new ECPrivateKeySpec(new BigInteger(1, scalar), P256));
    }

    static byte[] encodePublicKey(ECPublicKey key) {
        return concat(new byte[]{0x04}, unsigned32(key.getW().getAffineX()), unsigned32(key.getW().getAffineY()));
    }

    static byte[] encodePrivateKey(ECPrivateKey key) {
        return unsigned32(key.getS());
    }

    static byte[] base64UrlDecode(String value) {
        return Base64.getUrlDecoder().decode(value.trim().replace('+', '-').replace('/', '_').replace("=", ""));
    }

    static String base64UrlEncode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private static byte[] unsigned32(BigInteger value) {
        byte[] raw = value.toByteArray();
        byte[] out = new byte[32];
        int copy = Math.min(raw.length, 32);
        System.arraycopy(raw, raw.length - copy, out, 32 - copy, copy);
        return out;
    }

    private static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] part : parts) {
            out.writeBytes(part);
        }
        return out.toByteArray();
    }

    private static ECParameterSpec p256Parameters() {
        try {
            AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
            parameters.init(new ECGenParameterSpec("secp256r1"));
            return parameters.getParameterSpec(ECParameterSpec.class);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("P-256 is not available in this JVM", e);
        }
    }
}
