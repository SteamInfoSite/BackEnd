package com.steaminfosite.entity.user;

import com.steaminfosite.entity.savetime.BaseTime;
import com.steaminfosite.entity.type.SiteUserRole;
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

    public SiteUserEntity(String steamId) {
        if (steamId == null || steamId.isBlank()) {
            throw new IllegalArgumentException(
                    "Steam64 ID가 필요합니다"
            );
        }
        this.steamId = steamId;
        this.role = SiteUserRole.USER;
    }
}