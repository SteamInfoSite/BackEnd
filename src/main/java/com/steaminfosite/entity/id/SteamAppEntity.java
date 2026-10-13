package com.steaminfosite.entity.id;

import com.steaminfosite.entity.savetime.BaseTime;
import com.steaminfosite.entity.type.GameDetailStatus;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@ToString
@NoArgsConstructor
@Table(indexes = {@Index(
        name = "idx_steam_app_detail_status_app_id",    //detail_status로 수집 대상 필터링
        columnList = "detail_status, app_id"            //app_id 순서로 대상 조회
)})
public class SteamAppEntity extends BaseTime {  //appid 전체 저장용
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long appId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String name;

    @Column
    private Long lastModified;

    @Column
    private Long priceChangeNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private GameDetailStatus detailStatus;

    //상세API 오류가 발생한 누적 재시도 횟수
    @Column
    private Integer apiErrorRetryCount = 0;

    //API오류가 발생한 앱을 다시 요청할수 있는 시각
    @Column
    private LocalDateTime nextApiErrorRetryAt;

    @Builder
    public SteamAppEntity(Long appId, String name, Long lastModified, Long priceChangeNumber, GameDetailStatus detailStatus) {
        this.appId = appId;
        this.name = name;
        this.lastModified = lastModified;
        this.priceChangeNumber = priceChangeNumber;
        this.detailStatus = detailStatus;
    }

    public void updateFromSteam(
            String name,
            Long lastModified,
            Long priceChangeNumber
    ) {
        boolean changed = !Objects.equals(this.lastModified, lastModified)
                || !Objects.equals(
                        this.priceChangeNumber,
                        priceChangeNumber
                );

        this.name = name;
        this.lastModified = lastModified;
        this.priceChangeNumber = priceChangeNumber;

        if (changed) {
            //아직 한 번도 수집하지 않은 앱은 신규 수집 상태 유지
            if (this.detailStatus != GameDetailStatus.NOT_FETCHED) {
                this.detailStatus = GameDetailStatus.UPDATE_REQUIRED;
            }
            //새로운 변경이 확인됐으므로 재시도 기회를 다시 부여
            this.apiErrorRetryCount = 0;
            this.nextApiErrorRetryAt = null;
        }
    }

    public void updateDetailStatus(GameDetailStatus detailStatus) {
        this.detailStatus = detailStatus;
        //재시도가 성공했거나 미발견으로 확정되면 오류기록 초기화
        if (detailStatus == GameDetailStatus.SUCCESS || detailStatus == GameDetailStatus.NOT_FOUND) {
            this.apiErrorRetryCount = 0;
            this.nextApiErrorRetryAt = null;
        }
    }
    //API 또는 네트워크 오류를 기록하고 다음 재시도 시각을 지정
    public void recordApiError(long retryDelayMinutes) {
        int currentRetryCount = this.apiErrorRetryCount == null ? 0 : this.apiErrorRetryCount;
        this.apiErrorRetryCount = currentRetryCount + 1;
        this.nextApiErrorRetryAt = LocalDateTime.now().plusMinutes(retryDelayMinutes);
        this.detailStatus = GameDetailStatus.API_ERROR;
    }
}
