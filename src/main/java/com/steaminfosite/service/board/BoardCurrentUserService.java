package com.steaminfosite.service.board;

import com.steaminfosite.entity.user.SiteUserEntity;
import com.steaminfosite.repository.SiteUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class BoardCurrentUserService {
    private final SiteUserRepository users;
    public SiteUserEntity requireUser(String sessionSteamId){
        if (sessionSteamId == null || sessionSteamId.isBlank()){
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
        }
        return users.findBySteamId(sessionSteamId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "로그인 사용자를 찾을 수 없습니다."));
    }
}
