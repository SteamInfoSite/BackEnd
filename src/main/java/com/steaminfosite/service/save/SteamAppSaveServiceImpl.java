package com.steaminfosite.service.save;

import com.steaminfosite.dto.applist.SteamAppDto;
import com.steaminfosite.repository.SteamAppRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.steaminfosite.entity.id.SteamAppEntity;
import com.steaminfosite.entity.type.GameDetailStatus;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SteamAppSaveServiceImpl implements SteamAppSaveService {
    private final SteamAppRepository steamAppRepository;

    //한페이지 단위로 저장 트랜잭션을 묶고 도중에 멈춰도 이전 페이지 저장분은 남도록 분리
    @Transactional
    @Override
    public int saveOrUpdateSteamApps(List<SteamAppDto> apps) {
        int savedCount = 0;

        for (SteamAppDto app : apps) {
            saveOrUpdateSteamApp(app);
            savedCount++;
        }

        return savedCount;
    }

    //appId기준 이미 존재하면 최신정보로 갱신하고 없으면 새목록 데이터로 저장
    private void saveOrUpdateSteamApp(SteamAppDto app) {
        steamAppRepository.findByAppId(app.getAppId())
                .ifPresentOrElse(
                        savedApp -> savedApp.updateFromSteam(
                                app.getName(),
                                app.getLastModified(),
                                app.getPriceChangeNumber()
                        ),
                        () -> steamAppRepository.save(
                                SteamAppEntity.builder()
                                        .appId(app.getAppId())
                                        .name(app.getName())
                                        .lastModified(app.getLastModified())
                                        .priceChangeNumber(app.getPriceChangeNumber())
                                        .detailStatus(GameDetailStatus.NOT_FETCHED)
                                        .build()
                        )
                );
    }
}
