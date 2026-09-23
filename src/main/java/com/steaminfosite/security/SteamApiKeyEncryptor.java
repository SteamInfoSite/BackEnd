package com.steaminfosite.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;

@Component
public class SteamApiKeyEncryptor {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int KEY_LENGTH_BYTES = 32;
    private static final int IV_LENGTH_BYTES = 12;
    private static final int TAG_LENGTH_BITS = 128;

    private final SecretKey secretKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public SteamApiKeyEncryptor(
            @Value("${steam.security.api-key-encryption-key:}")
            String encodedKey
    ) {
        if (encodedKey == null || encodedKey.isBlank()) {
            throw new IllegalStateException(
                    "STEAM_API_KEY_ENCRYPTION_KEY가 설정되지 않았습니다"
            );
        }

        byte[] keyBytes;

        try {
            keyBytes = Base64.getDecoder().decode(encodedKey);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "API 키 암호화 키가 Base64 형식이 아닙니다",
                    exception
            );
        }

        if (keyBytes.length != KEY_LENGTH_BYTES) {
            throw new IllegalStateException(
                    "API키 암호화 키는 32바이트여야 합니다"
            );
        }

        this.secretKey = new SecretKeySpec(keyBytes, "AES");
    }

    public String encrypt(String plainText) {
        if (plainText == null || plainText.isBlank()) {
            throw new IllegalArgumentException(
                    "암호화할 Steam API 키가 필요합니다"
            );
        }

        byte[] iv = new byte[IV_LENGTH_BYTES];
        secureRandom.nextBytes(iv);

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    secretKey,
                    new GCMParameterSpec(TAG_LENGTH_BITS, iv)
            );

            byte[] encryptedBytes = cipher.doFinal(
                    plainText.getBytes(StandardCharsets.UTF_8)
            );

            byte[] result = ByteBuffer
                    .allocate(iv.length + encryptedBytes.length)
                    .put(iv)
                    .put(encryptedBytes)
                    .array();

            return Base64.getEncoder().encodeToString(result);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(
                    "스팀API 키 암호화에 실패했습니다",
                    exception
            );
        }
    }

    public String decrypt(String encryptedText) {
        if (encryptedText == null || encryptedText.isBlank()) {
            throw new IllegalArgumentException(
                    "복호화할 스팀API 키가 필요합니다"
            );
        }
        final byte[] storedBytes;

        try {
            storedBytes = Base64.getDecoder().decode(encryptedText);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "암호화된 스팀API 키 형식이 올바르지 않습니다",
                    exception
            );
        }
        if (storedBytes.length <= IV_LENGTH_BYTES) {
            throw new IllegalArgumentException(
                    "암호화된 스팀API 키 데이터가 올바르지 않습니다"
            );
        }
        ByteBuffer buffer = ByteBuffer.wrap(storedBytes);

        byte[] iv = new byte[IV_LENGTH_BYTES];
        buffer.get(iv);

        byte[] encryptedBytes = new byte[buffer.remaining()];
        buffer.get(encryptedBytes);

        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    secretKey,
                    new GCMParameterSpec(TAG_LENGTH_BITS, iv)
            );
            byte[] decryptedBytes = cipher.doFinal(encryptedBytes);

            return new String(
                    decryptedBytes,
                    StandardCharsets.UTF_8
            );
        } catch (GeneralSecurityException exception) {
            throw new IllegalArgumentException(
                    "스팀API 키 복호화에 실패했습니다",
                    exception
            );
        }
    }
}