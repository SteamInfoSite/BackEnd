package com.steaminfosite.service.user;

import com.steaminfosite.client.SteamUserApiClient;
import com.steaminfosite.entity.type.SteamApiKeyStatus;
import com.steaminfosite.entity.user.SiteUserEntity;
import com.steaminfosite.repository.SiteUserRepository;
import com.steaminfosite.security.SteamApiKeyEncryptor;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SteamApiKeyService {
    private final SiteUserRepository siteUserRepository;
    private final SteamUserApiClient steamUserApiClient;
    private final SteamApiKeyEncryptor steamApiKeyEncryptor;

    @Transactional
    public SteamApiKeyStatus register(
            String steamId,
            String steamApiKey
    ) {
        if (steamId == null || steamId.isBlank()) {
            throw new IllegalArgumentException(
                    "Steam64 ID가 필요합니다"
            );
        }
        if (steamApiKey == null || steamApiKey.isBlank()) {
            throw new IllegalArgumentException(
                    "스팀API키가 필요합니다"
            );
        }
        SiteUserEntity siteUser = siteUserRepository
                .findBySteamId(steamId)
                .orElseThrow(() -> new IllegalStateException(
                        "로그인 사용자를 찾을 수 없습니다"
                ));
        boolean valid = steamUserApiClient.validateApiKey(
                steamApiKey,
                steamId
        );
        if (!valid) {
            //유효하지 않은 키는 저장하지 않고 상태만 INVALID로 변경
            siteUser.markSteamApiKeyInvalid();

            return SteamApiKeyStatus.INVALID;
        }
        //유효성이 확인된 평문 키만 암호화
        String encryptedApiKey = steamApiKeyEncryptor.encrypt(steamApiKey);
        //엔티티에는 암호화된 값만 전달
        siteUser.registerSteamApiKey(encryptedApiKey);

        return SteamApiKeyStatus.VALID;
    }
}