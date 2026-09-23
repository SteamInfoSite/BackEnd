package com.steaminfosite.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SteamApiKeyEncryptorTest {
    private static final String TEST_ENCRYPTION_KEY =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private SteamApiKeyEncryptor encryptor;

    @BeforeEach
    void setUp() {
        encryptor = new SteamApiKeyEncryptor(TEST_ENCRYPTION_KEY);
    }

    @Test
    @DisplayName("암호화한 스팀API 키를 원래 값으로 복호화")
    void encryptAndDecrypt() {
        String steamApiKey = "test-steam-api-key";

        String encryptedApiKey = encryptor.encrypt(steamApiKey);
        String decryptedApiKey = encryptor.decrypt(encryptedApiKey);

        assertThat(encryptedApiKey).isNotEqualTo(steamApiKey);
        assertThat(decryptedApiKey).isEqualTo(steamApiKey);
    }

    @Test
    @DisplayName("같은 API키를 암호화해도 서로 다른 암호문이 생성")
    void encryptTwice() {
        String steamApiKey = "test-steam-api-key";

        String firstEncryptedApiKey = encryptor.encrypt(steamApiKey);
        String secondEncryptedApiKey = encryptor.encrypt(steamApiKey);

        assertThat(firstEncryptedApiKey).isNotEqualTo(secondEncryptedApiKey);
        assertThat(encryptor.decrypt(firstEncryptedApiKey)).isEqualTo(steamApiKey);
        assertThat(encryptor.decrypt(secondEncryptedApiKey)).isEqualTo(steamApiKey);
    }

    @Test
    @DisplayName("변조된 암호문은 복호화할 수 없다")
    void rejectTamperedText() {
        String encryptedApiKey = encryptor.encrypt("test-steam-api-key");
        byte[] encryptedBytes = Base64.getDecoder().decode(encryptedApiKey);
        encryptedBytes[encryptedBytes.length - 1] ^= 1;
        String tamperedApiKey = Base64.getEncoder().encodeToString(encryptedBytes);

        assertThatThrownBy(
                () -> encryptor.decrypt(tamperedApiKey)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("스팀API 키 복호화에 실패했습니다");
    }
}