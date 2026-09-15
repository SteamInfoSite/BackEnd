package com.steaminfosite.service.ratelimit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

//API요청 간격 전역 관리
@Component
public class SteamRequestRateLimiter {
    //yaml에서 주입받는 요청 속도 조절 기준값
    private final long minDelayMillis;
    private final long maxDelayMillis;
    private final int successThreshold;
    private final long decreaseStepMillis;
    private final long increaseStepMillis;

    //계속 변경되는 현재 요청 상태
    private long currentDelayMillis;
    private long nextRequestAtMillis;
    private int consecutiveSuccessCount;
    private int rateLimitCount;

    public SteamRequestRateLimiter(
            @Value("${steam.collector.initial-delay-ms:1000}")
            long initialDelayMillis,

            @Value("${steam.collector.min-delay-ms:500}")
            long minDelayMillis,

            @Value("${steam.collector.max-delay-ms:3000}")
            long maxDelayMillis,

            @Value("${steam.collector.success-threshold:100}")
            int successThreshold,

            @Value("${steam.collector.decrease-step-ms:100}")
            long decreaseStepMillis,

            @Value("${steam.collector.increase-step-ms:250}")
            long increaseStepMillis
    ) {
        this.currentDelayMillis = initialDelayMillis;
        this.minDelayMillis = minDelayMillis;
        this.maxDelayMillis = maxDelayMillis;
        this.successThreshold = successThreshold;
        this.decreaseStepMillis = decreaseStepMillis;
        this.increaseStepMillis = increaseStepMillis;
    }
    //여러 수집 작업이 동시에 실행돼도 요청 시각과 간격 상태가 충돌하지 않도록 동기화
    //다음 Steam API 요청이 가능한 시각까지 현재 스레드를 대기시킴
    public synchronized void waitBeforeRequest() {
        long now = System.currentTimeMillis();
        long waitMillis = nextRequestAtMillis - now;

        if (waitMillis > 0) {
            sleep(waitMillis);
        }
        nextRequestAtMillis = System.currentTimeMillis() + currentDelayMillis;
    }

    //정상 응답이 일정 횟수 누적되면 요청 간격을 조금씩 줄임
    public synchronized void recordSuccess() {
        consecutiveSuccessCount++;

        if (consecutiveSuccessCount < successThreshold) {
            return;
        }
        long previousDelay = currentDelayMillis;

        currentDelayMillis = Math.max(
                minDelayMillis,
                currentDelayMillis - decreaseStepMillis
        );
        consecutiveSuccessCount = 0;

        if (rateLimitCount > 0) {
            rateLimitCount--;
        }
        if (previousDelay != currentDelayMillis) {
            System.out.println(
                    "Steam API 요청 간격 감소: "
                            + previousDelay
                            + "ms -> "
                            + currentDelayMillis
                            + "ms"
            );
        }
    }

    //429 발생 시 요청 간격과 전체 요청 중단 시간을 함께 증가시킴
    public synchronized void recordRateLimited(long retryAfterMillis) {
        consecutiveSuccessCount = 0;
        rateLimitCount++;

        currentDelayMillis = Math.min(
                maxDelayMillis,
                currentDelayMillis + increaseStepMillis
        );

        //누적된 429단계에 따라 6초, 12초, 24초 순으로 대기
        long multiplier = 1L << Math.min(rateLimitCount - 1, 2);

        long calculatedCooldownMillis = 6_000L * multiplier;

        long cooldownMillis = Math.max(
                retryAfterMillis,
                calculatedCooldownMillis
        );
        nextRequestAtMillis = Math.max(
                nextRequestAtMillis,
                System.currentTimeMillis() + cooldownMillis
        );
        System.out.println(
                "Steam API 429 발생: "
                        + cooldownMillis
                        + "ms 대기, 이후 요청 간격="
                        + currentDelayMillis
                        + "ms"
        );
    }

    private void sleep(long waitMillis) {
        try {
            Thread.sleep(waitMillis);
        } catch (InterruptedException e) {
            //InterruptedException 발생 시 해제된 인터럽트 상태를 복원, 현재 작업 중단 확인
            Thread.currentThread().interrupt();

            throw new IllegalStateException(
                    "Steam API 요청 대기 중 스레드가 중단되었습니다",
                    e
            );
        }
    }

    public synchronized long getCurrentDelayMillis() {
        return currentDelayMillis;
    }
}
