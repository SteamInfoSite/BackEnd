package com.steaminfosite.service.save;

import com.steaminfosite.dto.api.price.SteamPriceOverview;
import com.steaminfosite.dto.api.price.SteamPriceWrapper;
import com.steaminfosite.entity.price.GamePriceEntity;
import com.steaminfosite.repository.GamePriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GamePriceSaveServiceImpl implements GamePriceSaveService {

    private final GamePriceRepository gamePriceRepository;

    @Override
    @Transactional
    public int saveOrUpdatePrices(
            Collection<Long> requestedAppIds,
            Map<String, SteamPriceWrapper> responses,
            Map<Long, Long> priceChangeNumbers
    ) {
        if (requestedAppIds == null || responses == null || priceChangeNumbers == null) {
            throw new IllegalArgumentException(
                    "요청한 게임 ID와 가격 응답이 필요합니다"
            );
        }

        if (requestedAppIds.isEmpty()) {
            return 0;
        }

        if (requestedAppIds.stream().anyMatch(
                appId -> appId == null || appId <= 0
        )) {
            throw new IllegalArgumentException(
                    "올바른 Steam 게임 ID가 필요합니다"
            );
        }
        List<Long> appIds = requestedAppIds.stream().distinct().toList();
        //기존 가격을 한 번의 IN 조회로 가져옴
        Map<Long, GamePriceEntity> existingPrices = new HashMap<>();

        for (GamePriceEntity price :
                gamePriceRepository.findAllByAppIdIn(appIds)) {
            existingPrices.put(price.getAppId(), price);
        }

        List<GamePriceEntity> pricesToSave = new ArrayList<>();

        for (Long appId : appIds) {
            SteamPriceWrapper wrapper = responses.get(String.valueOf(appId));
            //실패하거나 누락된 응답은 기존 데이터를 유지
            if (wrapper == null
                    || !Boolean.TRUE.equals(wrapper.getSuccess())
                    || wrapper.getData() == null) {
                System.out.println(
                        "가격 저장 건너뜀: 응답 실패 또는 누락 appId="
                                + appId
                );
                continue;
            }
            SteamPriceOverview overview = wrapper.getData().getPriceOverview();
            //가격 객체가 있다면 필수 값부터 검사
            if (overview != null && !isValidPrice(overview)) {
                System.out.println(
                        "가격 저장 건너뜀: 잘못된 가격 정보 appId="
                                + appId
                );
                continue;
            }
            GamePriceEntity entity = existingPrices.get(appId);

            if (entity == null) {
                entity = new GamePriceEntity(appId);
            }

            if (overview == null) {
                // 정상 응답이지만 가격이 제공되지 않음
                entity.markPriceUnavailable();
            } else {
                entity.updatePrice(
                        overview.getCurrency(),
                        overview.getInitialAmount(),
                        overview.getFinalAmount(),
                        overview.getDiscountPercent(),
                        overview.getInitialFormatted(),
                        overview.getFinalFormatted()
                );
            }
            Long priceChangeNumber = priceChangeNumbers.get(appId);

            if (priceChangeNumber != null) {
                entity.recordCollectedPriceChangeNumber(priceChangeNumber);
            }
            pricesToSave.add(entity);
        }
        if (!pricesToSave.isEmpty()) {
            gamePriceRepository.saveAll(pricesToSave);
        }
        //신규 저장과 정상 확인·갱신을 포함한 처리 개수
        return pricesToSave.size();
    }

    private boolean isValidPrice(SteamPriceOverview price) {
        // 현재 저장 대상은 한국 원화 가격
        if (!"KRW".equals(price.getCurrency())) {
            return false;
        }
        Long initialAmount = price.getInitialAmount();
        Long finalAmount = price.getFinalAmount();
        Integer discountPercent = price.getDiscountPercent();

        if (initialAmount == null
                || finalAmount == null
                || discountPercent == null) {
            return false;
        }
        if (initialAmount < 0
                || finalAmount < 0
                || finalAmount > initialAmount
                || discountPercent < 0
                || discountPercent > 100) {
            return false;
        }
        String finalFormatted = price.getFinalFormatted();

        return finalFormatted != null && !finalFormatted.isBlank();
    }
}