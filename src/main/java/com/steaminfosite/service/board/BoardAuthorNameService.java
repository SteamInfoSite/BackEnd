package com.steaminfosite.service.board;

import com.steaminfosite.client.SteamProfileClient;
import com.steaminfosite.exception.SteamApiException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BoardAuthorNameService {
    public static final String FALLBACK_NAME = "Steam 사용자";

    private static final Logger log =
            LoggerFactory.getLogger(BoardAuthorNameService.class);

    private final SteamProfileClient steamProfileClient;

    public String getDisplayName(String steamId) {
        try {
            return steamProfileClient.getDisplayName(steamId);
        } catch (SteamApiException exception) {
            log.warn("Steam 작성자 이름 조회에 실패해 기본 이름을 사용합니다.");
            return FALLBACK_NAME;
        }
    }

    public Map<String, String> getDisplayNames(Collection<String> steamIds) {
        try {
            return steamProfileClient.getDisplayNames(steamIds);
        } catch (SteamApiException exception) {
            log.warn("Steam 작성자 이름 목록 조회에 실패해 기본 이름을 사용합니다.");
            return Map.of();
        }
    }
}