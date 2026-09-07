package com.steaminfosite.service;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.steaminfosite.client.SteamApiClient;
import com.steaminfosite.dto.api.SteamApiData;
import com.steaminfosite.dto.api.depth1.ReleaseDate;
import com.steaminfosite.dto.response.GameInfoResponse;
import com.steaminfosite.entity.id.GameInfoEntity;
import com.steaminfosite.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SteamApiServiceImpl implements SteamApiService {

    private final SteamApiClient steamApiClient;
    private final GameRepository gameRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    String appId = "4787850";

    @Override
    public GameInfoResponse getGameInfo(long appId){
        Optional<GameInfoEntity> savedGame = gameRepository.findByAppId(appId);
        if(savedGame.isPresent()){  //appId가 들어가있는지 확인하고 중복저장 방지
            return GameInfoResponse.from(savedGame.get());
        }

        Optional<SteamApiData> gameData = steamApiClient.fetchAppDetails(appId);
        if (gameData.isEmpty()){
            throw (new IllegalArgumentException("게임 데이터를 받아올 수 없습니다."));
        }
        SteamApiData result = gameData.get();
        ReleaseDate releaseDate = result.getReleaseDate();

        GameInfoEntity gameInfoEntity = GameInfoEntity.builder()
                .appId(result.getSteamAppId())
                .gameName(result.getName())
                .type(result.getType())
                .requiredAge(result.getRequiredAge())
                .isFree(result.getIsFree())
                .shortDescription(result.getShortDescription())
                .headerImage(result.getHeaderImage())
                .capsuleImage(result.getCapsuleImage())
                .comingSoon(releaseDate != null ? releaseDate.getComingSoon() : null)
                .relDate(releaseDate != null ? releaseDate.getRelDate() : null)
                .build();
        return GameInfoResponse.from(gameRepository.save(gameInfoEntity));
    }

    @Override
    public String getJsonDetails(){
        String url = "https://store.steampowered.com/api/appdetails?appids=" + appId;
        ResponseEntity<String> responseEntity = restTemplate.getForEntity(url, String.class);
        String gameData = responseEntity.getBody();
        return gameData;
    }

    @Override
    public GameInfoResponse parseJson(String gameData) throws Exception{
        JsonNode json = objectMapper.readTree(gameData);

        JsonNode node1 = json.path("4787850");
        JsonNode node2 = node1.path("data");
        Long appId = node2.path("steam_appid").asLong();
        String gameName = node2.path("name").asText();

        GameInfoEntity gameInfo = GameInfoEntity.builder()
                .appId(appId)
                .gameName(gameName)
                .build();
        return GameInfoResponse.from(gameInfo);
    }


}


//https://store.steampowered.com/api/appdetails?appids=730