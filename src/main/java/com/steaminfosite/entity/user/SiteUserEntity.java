package com.steaminfosite.entity.user;

import com.steaminfosite.entity.savetime.BaseTime;
import com.steaminfosite.entity.type.SiteUserRole;
import com.steaminfosite.entity.type.SteamApiKeyStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "site_user")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SiteUserEntity extends BaseTime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //스팀 로그인으로 확인한 사용자의 고유 Steam64 ID
    @Column(
            name = "steam_id",
            nullable = false,
            unique = true,
            updatable = false,
            length = 20
    )
    private String steamId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SiteUserRole role;

    //평문이 아닌 암호화된 스팀API 키만 저장
    @Column(
            name = "encrypted_steam_api_key",
            columnDefinition = "TEXT"
    )
    private String encryptedSteamApiKey;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "steam_api_key_status",
            nullable = false,
            length = 20
    )
    private SteamApiKeyStatus steamApiKeyStatus = SteamApiKeyStatus.NOT_REGISTERED;

    public SiteUserEntity(String steamId) {
        if (steamId == null || steamId.isBlank()) {
            throw new IllegalArgumentException(
                    "Steam64 ID가 필요합니다"
            );
        }
        this.steamId = steamId;
        this.role = SiteUserRole.USER;
        this.steamApiKeyStatus = SteamApiKeyStatus.NOT_REGISTERED;
    }

    public void registerSteamApiKey(
            String encryptedSteamApiKey
    ) {
        if (encryptedSteamApiKey == null || encryptedSteamApiKey.isBlank()) {
            throw new IllegalArgumentException("암호화된 Steam API 키가 필요합니다");
        }

        this.encryptedSteamApiKey = encryptedSteamApiKey;
        this.steamApiKeyStatus = SteamApiKeyStatus.VALID;
    }

    public void markSteamApiKeyInvalid() {
        //유효하지 않은 키는 DB에 보관하지 않음
        this.encryptedSteamApiKey = null;
        this.steamApiKeyStatus = SteamApiKeyStatus.INVALID;
    }

    public void removeSteamApiKey() {
        this.encryptedSteamApiKey = null;
        this.steamApiKeyStatus = SteamApiKeyStatus.NOT_REGISTERED;
    }
}