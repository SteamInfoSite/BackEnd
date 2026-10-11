package com.steaminfosite.entity.price;

import com.steaminfosite.entity.savetime.BaseTime;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@Table(name = "game_price")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GamePriceEntity extends BaseTime {

    //스팀 게임 ID를 그대로 사용
    @Id
    @Column(name = "app_id", nullable = false)
    private Long appId;

    //현재는 한국 스토어 가격만 저장
    @Column(nullable = false, length = 2)
    private String countryCode = "KR";

    @Column(length = 3)
    private String currency;

    //스팀에서 받은 금액 원본
    private Long initialAmount;
    private Long finalAmount;

    private Integer discountPercent;

    //화면 표시용 가격 문자열
    @Column(length = 100)
    private String initialFormatted;

    @Column(length = 100)
    private String finalFormatted;

    @Column(nullable = false)
    private boolean priceAvailable;

    //가격 제공 여부를 정상적으로 확인한 시각
    @Column(nullable = false)
    private LocalDateTime priceCheckedAt;

    public GamePriceEntity(Long appId) {
        if (appId == null || appId <= 0) {
            throw new IllegalArgumentException(
                    "올바른 스팀 게임 ID가 필요합니다"
            );
        }
        this.appId = appId;
    }

    public void updatePrice(
            String currency,
            Long initialAmount,
            Long finalAmount,
            Integer discountPercent,
            String initialFormatted,
            String finalFormatted
    ) {
        this.currency = currency;
        this.initialAmount = initialAmount;
        this.finalAmount = finalAmount;
        this.discountPercent = discountPercent;
        this.initialFormatted = initialFormatted;
        this.finalFormatted = finalFormatted;
        this.priceAvailable = true;
        this.priceCheckedAt = LocalDateTime.now();
    }

    public void markPriceUnavailable() {
        this.currency = null;
        this.initialAmount = null;
        this.finalAmount = null;
        this.discountPercent = null;
        this.initialFormatted = null;
        this.finalFormatted = null;
        this.priceAvailable = false;
        this.priceCheckedAt = LocalDateTime.now();
    }
}