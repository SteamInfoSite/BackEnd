package com.steaminfosite.mapper;

import com.steaminfosite.dto.response.GameInfoResponse.Platforms;
import com.steaminfosite.dto.response.GameListResponse;
import com.steaminfosite.dto.response.GamePriceResponse;
import com.steaminfosite.entity.id.GameGenreEntity;
import com.steaminfosite.entity.id.GameInfoEntity;
import org.springframework.stereotype.Component;

@Component
public class GameListResponseMapper {
    public GameListResponse.Item toItem(
            GameInfoEntity game,
            GamePriceResponse price
    ) {
        return GameListResponse.Item.builder()
                .appId(game.getAppId())
                .gameName(game.getGameName())
                .headerImage(game.getHeaderImage())
                .capsuleImage(game.getCapsuleImage())
                .isFree(game.getIsFree())
                .comingSoon(game.getComingSoon())
                .relDate(game.getRelDate())
                .genres(
                        game.getGenres().stream()
                                .map(GameGenreEntity::getDescription)
                                .toList()
                )
                .platforms(
                        Platforms.builder()
                                .windows(game.getWindows())
                                .mac(game.getMac())
                                .linux(game.getLinux())
                                .build()
                )
                .recommendationsTotal(game.getRecommendationsTotal())
                .updatedAt(game.getUpdatedAt())
                .price(price)
                .build();
    }
}