package com.steaminfosite.service.user;

import com.steaminfosite.entity.user.SiteUserEntity;
import com.steaminfosite.repository.SiteUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SiteUserService {

    private final SiteUserRepository siteUserRepository;

    @Transactional
    public SiteUserEntity findOrCreate(String steamId) {
        if (steamId == null || steamId.isBlank()) {
            throw new IllegalArgumentException(
                    "Steam64 ID가 필요합니다"
            );
        }

        return siteUserRepository.findBySteamId(steamId)
                .orElseGet(() -> siteUserRepository.save(
                        new SiteUserEntity(steamId) //entity에 저장되지 않은 사용자의 경우 저장
                ));                                 //이미 있는 경우 기존 entity 반환
    }
}
