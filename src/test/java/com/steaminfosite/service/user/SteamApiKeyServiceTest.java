package com.steaminfosite.service.user;

import com.steaminfosite.client.SteamUserApiClient;
import com.steaminfosite.entity.type.SteamApiKeyStatus;
import com.steaminfosite.entity.user.SiteUserEntity;
import com.steaminfosite.exception.SteamApiException;
import com.steaminfosite.repository.SiteUserRepository;
import com.steaminfosite.security.SteamApiKeyEncryptor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class SteamApiKeyServiceTest {
    private static final String STEAM_ID = "76561198000000000";
    private static final String API_KEY = "test-steam-api-key";
    private static final String ENCRYPTED_API_KEY = "encrypted-api-key";

    @Mock
    private SiteUserRepository siteUserRepository;

    @Mock
    private SteamUserApiClient steamUserApiClient;

    @Mock
    private SteamApiKeyEncryptor steamApiKeyEncryptor;

    @InjectMocks
    private SteamApiKeyService steamApiKeyService;

    @Test
    @DisplayName("유효한 스팀API키를 암호화하여 등록")
    void registerValidKey() {
        SiteUserEntity siteUser = new SiteUserEntity(STEAM_ID);
        given(siteUserRepository.findBySteamId(STEAM_ID)).willReturn(Optional.of(siteUser));
        given(steamUserApiClient.validateApiKey(
                API_KEY,
                STEAM_ID
        )).willReturn(true);
        given(steamApiKeyEncryptor.encrypt(API_KEY)).willReturn(ENCRYPTED_API_KEY);

        SteamApiKeyStatus result = steamApiKeyService.register(
                STEAM_ID,
                API_KEY
        );

        assertThat(result).isEqualTo(SteamApiKeyStatus.VALID);
        assertThat(siteUser.getSteamApiKeyStatus()).isEqualTo(SteamApiKeyStatus.VALID);
        assertThat(siteUser.getEncryptedSteamApiKey()).isEqualTo(ENCRYPTED_API_KEY);
        then(steamApiKeyEncryptor).should().encrypt(API_KEY);
    }

    @Test
    @DisplayName("유효하지 않은 스팀API키는 저장하지 않음")
    void rejectInvalidKey() {
        SiteUserEntity siteUser = new SiteUserEntity(STEAM_ID);
        siteUser.registerSteamApiKey("previous-encrypted-key");
        given(siteUserRepository.findBySteamId(STEAM_ID)).willReturn(Optional.of(siteUser));
        given(steamUserApiClient.validateApiKey(
                API_KEY,
                STEAM_ID
        )).willReturn(false);

        SteamApiKeyStatus result = steamApiKeyService.register(
                STEAM_ID,
                API_KEY
        );

        assertThat(result).isEqualTo(SteamApiKeyStatus.INVALID);
        assertThat(siteUser.getSteamApiKeyStatus()).isEqualTo(SteamApiKeyStatus.INVALID);
        assertThat(siteUser.getEncryptedSteamApiKey()).isNull();
        then(steamApiKeyEncryptor).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("스팀 서버 오류가 발생하면 사용자 키 상태를 변경하지 않음")
    void keepStatusOnServerError() {
        SiteUserEntity siteUser = new SiteUserEntity(STEAM_ID);
        given(siteUserRepository.findBySteamId(STEAM_ID)).willReturn(Optional.of(siteUser));
        given(steamUserApiClient.validateApiKey(
                API_KEY,
                STEAM_ID
        )).willThrow(new SteamApiException(
                "스팀 서버 오류",
                new RuntimeException()
        ));

        assertThatThrownBy(
                () -> steamApiKeyService.register(
                        STEAM_ID,
                        API_KEY
                )
        ).isInstanceOf(SteamApiException.class).hasMessage("스팀 서버 오류");
        assertThat(siteUser.getSteamApiKeyStatus())
                .isEqualTo(SteamApiKeyStatus.NOT_REGISTERED);
        assertThat(siteUser.getEncryptedSteamApiKey()).isNull();
        then(steamApiKeyEncryptor).shouldHaveNoInteractions();
    }
}