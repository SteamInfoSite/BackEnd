package com.steaminfosite.service.save;

import com.steaminfosite.dto.applist.SteamAppDto;
import com.steaminfosite.repository.SteamAppRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.steaminfosite.entity.id.SteamAppEntity;
import com.steaminfosite.entity.type.GameDetailStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SteamAppSaveServiceImpl implements SteamAppSaveService {
    private final SteamAppRepository steamAppRepository;

    //한페이지 단위로 저장 트랜잭션을 묶고 도중에 멈춰도 이전 페이지 저장분은 남도록 분리
    @Transactional
    @Override
    public int saveOrUpdateSteamApps(List<SteamAppDto> apps) {
        if (apps == null || apps.isEmpty()) {
            return 0;
        }
        //이번 페이지에서 받은 appId 목록
        List<Long> appIds = apps.stream()
                .map(SteamAppDto::getAppId)
                .toList();

        //이미DB에 저장된앱을 한번의 IN쿼리로 조회
        Map<Long, SteamAppEntity> savedAppMap =
                steamAppRepository.findAllByAppIdIn(appIds).stream()
                        .collect(Collectors.toMap(
                                SteamAppEntity::getAppId,
                                Function.identity()
                        ));

        List<SteamAppEntity> saveTargets = new ArrayList<>();

        for (SteamAppDto app : apps) {
            SteamAppEntity savedApp = savedAppMap.get(app.getAppId());

            if (savedApp != null) {
                //기존앱은 최신목록 정보로 갱신
                //변경이 감지되면 updateFromSteam내부에서 UPDATE_REQUIRED로 전환
                savedApp.updateFromSteam(
                        app.getName(),
                        app.getLastModified(),
                        app.getPriceChangeNumber()
                );

                saveTargets.add(savedApp);
            } else {
                //처음 발견한 앱은 상세정보 미수집 상태로 생성
                SteamAppEntity newApp = SteamAppEntity.builder()
                        .appId(app.getAppId())
                        .name(app.getName())
                        .lastModified(app.getLastModified())
                        .priceChangeNumber(app.getPriceChangeNumber())
                        .detailStatus(GameDetailStatus.NOT_FETCHED)
                        .build();
                saveTargets.add(newApp);
            }
        }
        //신규 앱과 갱신된 앱을 한번에 저장
        steamAppRepository.saveAll(saveTargets);
        return saveTargets.size();
    }
}
