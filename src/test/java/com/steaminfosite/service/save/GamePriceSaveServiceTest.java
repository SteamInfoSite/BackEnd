package com.steaminfosite.service.save;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.steaminfosite.dto.api.price.SteamPriceWrapper;
import com.steaminfosite.entity.price.GamePriceEntity;
import com.steaminfosite.repository.GamePriceRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class GamePriceSaveServiceTest {

    private static final Long APP_ID = 620L;

    private static final LocalDateTime OLD_CHECKED_AT =
            LocalDateTime.of(2020, 1, 1, 0, 0);

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private GamePriceRepository gamePriceRepository;

    @InjectMocks
    private GamePriceSaveServiceImpl gamePriceSaveService;

    @Captor
    private ArgumentCaptor<List<GamePriceEntity>> savedPricesCaptor;

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("정상 가격은 신규 저장하거나 기존 가격을 갱신")
    void saveOrUpdateValidPrice(boolean alreadyExists) throws Exception {
        GamePriceEntity existing = createExistingPrice();

        given(gamePriceRepository.findAllByAppIdIn(List.of(APP_ID)))
                .willReturn(
                        alreadyExists ? List.of(existing) : List.of()
                );

        var responses = readResponses("""
                {
                  "620": {
                    "success": true,
                    "data": {
                      "price_overview": {
                        "currency": "KRW",
                        "initial": 1100000,
                        "final": 1100000,
                        "discount_percent": 0,
                        "initial_formatted": "",
                        "final_formatted": "₩11,000"
                      }
                    }
                  }
                }
                """);

        int count = gamePriceSaveService.saveOrUpdatePrices(
                List.of(APP_ID),
                responses,
                Map.of(APP_ID, 10L)
        );

        then(gamePriceRepository).should()
                .saveAll(savedPricesCaptor.capture());

        assertThat(count).isEqualTo(1);
        assertThat(savedPricesCaptor.getValue()).hasSize(1);

        GamePriceEntity saved = savedPricesCaptor.getValue().get(0);

        if (alreadyExists) {
            assertThat(saved).isSameAs(existing);
        }

        assertThat(saved.getAppId()).isEqualTo(APP_ID);
        assertThat(saved.getCountryCode()).isEqualTo("KR");
        assertThat(saved.getCurrency()).isEqualTo("KRW");
        assertThat(saved.getInitialAmount()).isEqualTo(1100000L);
        assertThat(saved.getFinalAmount()).isEqualTo(1100000L);
        assertThat(saved.getDiscountPercent()).isZero();
        assertThat(saved.getInitialFormatted()).isEmpty();
        assertThat(saved.getFinalFormatted()).isEqualTo("₩11,000");
        assertThat(saved.isPriceAvailable()).isTrue();
        assertThat(saved.getPriceCheckedAt()).isAfter(OLD_CHECKED_AT);
        assertThat(saved.getCollectedPriceChangeNumber()).isEqualTo(10L);
    }

    @Test
    @DisplayName("정상적인 가격 미제공 응답은 이전 가격을 비운다")
    void markPriceUnavailable() throws Exception {
        GamePriceEntity existing = createExistingPrice();

        given(gamePriceRepository.findAllByAppIdIn(List.of(APP_ID)))
                .willReturn(List.of(existing));

        var responses = readResponses("""
                {
                  "620": {
                    "success": true,
                    "data": []
                  }
                }
                """);

        int count = gamePriceSaveService.saveOrUpdatePrices(
                List.of(APP_ID),
                responses,
                Map.of(APP_ID, 10L)
        );

        then(gamePriceRepository).should()
                .saveAll(savedPricesCaptor.capture());

        assertThat(count).isEqualTo(1);
        assertThat(savedPricesCaptor.getValue())
                .containsExactly(existing);

        assertThat(existing.isPriceAvailable()).isFalse();
        assertThat(existing.getCurrency()).isNull();
        assertThat(existing.getInitialAmount()).isNull();
        assertThat(existing.getFinalAmount()).isNull();
        assertThat(existing.getDiscountPercent()).isNull();
        assertThat(existing.getInitialFormatted()).isNull();
        assertThat(existing.getFinalFormatted()).isNull();
        assertThat(existing.getPriceCheckedAt()).isAfter(OLD_CHECKED_AT);
        assertThat(existing.getCollectedPriceChangeNumber()).isEqualTo(10L);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"620\":{\"success\":false}}",
            "{\"620\":{\"data\":[]}}",
            "{\"620\":{\"success\":true,\"data\":null}}",
            "{\"620\":{\"success\":true,\"data\":{\"price_overview\":{}}}}"
    })
    @DisplayName("실패·누락·불완전한 가격 응답은 기존 값을 유지한다")
    void preserveExistingPrice(String responseJson) throws Exception {
        GamePriceEntity existing = createExistingPrice();
        existing.recordCollectedPriceChangeNumber(9L);

        given(gamePriceRepository.findAllByAppIdIn(List.of(APP_ID)))
                .willReturn(List.of(existing));

        int count = gamePriceSaveService.saveOrUpdatePrices(
                List.of(APP_ID),
                readResponses(responseJson),
                Map.of(APP_ID, 10L)
        );

        assertThat(count).isZero();

        then(gamePriceRepository).should(never()).saveAll(any());

        //저장 호출뿐 아니라 엔티티 자체도 변경되지 않았는지 확인
        assertThat(existing.getCurrency()).isEqualTo("KRW");
        assertThat(existing.getInitialAmount()).isEqualTo(1100000L);
        assertThat(existing.getFinalAmount()).isEqualTo(220000L);
        assertThat(existing.getDiscountPercent()).isEqualTo(80);
        assertThat(existing.getInitialFormatted()).isEqualTo("₩11,000");
        assertThat(existing.getFinalFormatted()).isEqualTo("₩2,200");
        assertThat(existing.isPriceAvailable()).isTrue();
        assertThat(existing.getPriceCheckedAt()).isEqualTo(OLD_CHECKED_AT);
        assertThat(existing.getCollectedPriceChangeNumber()).isEqualTo(9L);
    }

    private GamePriceEntity createExistingPrice() {
        GamePriceEntity entity = new GamePriceEntity(APP_ID);

        entity.updatePrice(
                "KRW",
                1100000L,
                220000L,
                80,
                "₩11,000",
                "₩2,200"
        );

        //대기 없이 갱신 여부를 검증하도록 과거 시각을 설정
        ReflectionTestUtils.setField(
                entity,
                "priceCheckedAt",
                OLD_CHECKED_AT
        );

        return entity;
    }

    private Map<String, SteamPriceWrapper> readResponses(
            String json
    ) throws Exception {
        return objectMapper.readValue(
                json,
                new TypeReference<Map<String, SteamPriceWrapper>>() {}
        );
    }
}