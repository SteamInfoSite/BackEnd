package com.steaminfosite.service.price;

import com.steaminfosite.dto.response.GamePriceCollectResult;
import com.steaminfosite.repository.GameRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.stream.LongStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GamePriceInitialCollectServiceTest {

    private static final PageRequest PAGE =
            PageRequest.of(0, 100);

    @Mock
    private GameRepository gameRepository;

    @Mock
    private GamePriceCollectService gamePriceCollectService;

    @InjectMocks
    private GamePriceInitialCollectServiceImpl initialCollectService;

    @Test
    @DisplayName("미수집 게임이 없으면 수집기를 호출하지 않는다")
    void skipWhenNoTargets() {
        given(gameRepository.findUncollectedPriceAppIdsAfter(
                0L, PAGE
        )).willReturn(List.of());

        GamePriceCollectResult result =
                initialCollectService.collectInitialPrices();

        assertThat(result.savedCount()).isZero();
        assertThat(result.rateLimited()).isFalse();

        verifyNoInteractions(gamePriceCollectService);
    }

    @Test
    @DisplayName("일부 수집에 실패해도 다음 묶음으로 이동한다")
    void continueAfterPartialFailure() {
        List<Long> firstBatch = LongStream.rangeClosed(1, 100)
                .boxed()
                .toList();

        List<Long> secondBatch = List.of(101L, 102L);

        given(gameRepository.findUncollectedPriceAppIdsAfter(
                0L, PAGE
        )).willReturn(firstBatch);

        given(gameRepository.findUncollectedPriceAppIdsAfter(
                100L, PAGE
        )).willReturn(secondBatch);

        given(gameRepository.findUncollectedPriceAppIdsAfter(
                102L, PAGE
        )).willReturn(List.of());

        // 첫 100개 중 1개는 실패한 상황
        given(gamePriceCollectService.collectPrices(firstBatch))
                .willReturn(new GamePriceCollectResult(99, false));

        given(gamePriceCollectService.collectPrices(secondBatch))
                .willReturn(new GamePriceCollectResult(2, false));

        GamePriceCollectResult result = initialCollectService.collectInitialPrices();

        assertThat(result.savedCount()).isEqualTo(101);
        assertThat(result.rateLimited()).isFalse();

        then(gameRepository).should()
                .findUncollectedPriceAppIdsAfter(0L, PAGE);
        then(gameRepository).should()
                .findUncollectedPriceAppIdsAfter(100L, PAGE);
        then(gameRepository).should()
                .findUncollectedPriceAppIdsAfter(102L, PAGE);

        then(gamePriceCollectService).should().collectPrices(firstBatch);
        then(gamePriceCollectService).should().collectPrices(secondBatch);

        verifyNoMoreInteractions(
                gameRepository,
                gamePriceCollectService
        );
    }

    @Test
    @DisplayName("요청 제한 후 같은 위치에서 미처리 대상을 다시 수집한다")
    void resumeAfterRateLimit() {
        List<Long> firstBatch = List.of(10L, 20L, 30L);
        List<Long> remainingBatch = List.of(20L, 30L);

        given(gameRepository.findUncollectedPriceAppIdsAfter(
                0L, PAGE
        )).willReturn(
                firstBatch,      //첫 요청: 저장 전에 요청 제한
                firstBatch,      //재시도: 10번 저장 후 다시 요청 제한
                remainingBatch   //저장된 10번을 제외하고 다시 조회
        );

        given(gameRepository.findUncollectedPriceAppIdsAfter(
                30L, PAGE
        )).willReturn(List.of());

        given(gamePriceCollectService.collectPrices(firstBatch))
                .willReturn(
                        new GamePriceCollectResult(0, true),
                        new GamePriceCollectResult(1, true)
                );

        given(gamePriceCollectService.collectPrices(remainingBatch))
                .willReturn(new GamePriceCollectResult(2, false));

        GamePriceCollectResult result =
                initialCollectService.collectInitialPrices();

        assertThat(result.savedCount()).isEqualTo(3);
        assertThat(result.rateLimited()).isFalse();

        then(gameRepository).should(times(3))
                .findUncollectedPriceAppIdsAfter(0L, PAGE);

        then(gameRepository).should()
                .findUncollectedPriceAppIdsAfter(30L, PAGE);

        then(gamePriceCollectService).should(times(2))
                .collectPrices(firstBatch);

        then(gamePriceCollectService).should()
                .collectPrices(remainingBatch);

        verifyNoMoreInteractions(
                gameRepository,
                gamePriceCollectService
        );
    }
}