package com.steaminfosite.dto.response;

//한 번의 상세정보 수집 결과를 상태별 전달
public record GameDetailCollectResult(
        int successCount,
        int notFoundCount,
        int apiErrorCount,
        int rateLimitedCount
) {
    //API요청후 상태처리가 끝난 개수
    public int processedCount() {
        return successCount + notFoundCount + apiErrorCount;
    }

    //429포함한 실제 요청을 시도한 전체횟수
    public int attemptedCount() {
        return processedCount() + rateLimitedCount;
    }

    //여러번 나눠 수집한 결과를 하나로 합침
    public GameDetailCollectResult add(GameDetailCollectResult other) {
        return new GameDetailCollectResult(
                successCount + other.successCount,
                notFoundCount + other.notFoundCount,
                apiErrorCount + other.apiErrorCount,
                rateLimitedCount + other.rateLimitedCount
        );
    }

    public static GameDetailCollectResult empty() {
        return new GameDetailCollectResult(0, 0, 0, 0);
    }
}