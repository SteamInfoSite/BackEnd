package com.steaminfosite.service.save;

import com.steaminfosite.dto.api.SteamApiData;
import com.steaminfosite.dto.api.depth1.ReleaseDate;
import com.steaminfosite.dto.response.GameInfoResponse;
import com.steaminfosite.entity.id.GameInfoEntity;
import com.steaminfosite.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class GameInfoSaveServiceImpl implements GameInfoSaveService{
    private final GameRepository gameRepository;

    //상세정보는 조회만 하므로 readOnly 트랜잭션으로 불필요한 변경 감지를 줄임
    @Override
    @Transactional(readOnly = true)
    public Optional<GameInfoResponse> findSavedGameInfo(Long appId) {
        return gameRepository.findByAppId(appId)
                .map(GameInfoResponse::from);
    }

    //Steam 상세 API DTO를 GameInfoEntity로 변환한 뒤 DB에 저장
    @Override
    @Transactional
    public GameInfoResponse saveGameInfo(SteamApiData gameData) {
        ReleaseDate releaseDate = gameData.getReleaseDate();

        GameInfoEntity gameInfoEntity = GameInfoEntity.builder()
                .appId(gameData.getSteamAppId())
                .gameName(gameData.getName())
                .type(gameData.getType())
                .requiredAge(gameData.getRequiredAge())
                .isFree(gameData.getIsFree())
                .shortDescription(gameData.getShortDescription())
                .headerImage(gameData.getHeaderImage())
                .capsuleImage(gameData.getCapsuleImage())
                .comingSoon(releaseDate != null ? releaseDate.getComingSoon() : null)
                .relDate(releaseDate != null ? releaseDate.getRelDate() : null)
                .build();

        return GameInfoResponse.from(gameRepository.save(gameInfoEntity));
    }
}
