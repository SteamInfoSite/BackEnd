package com.steaminfosite.entity.id;

import com.steaminfosite.entity.savetime.BaseTime;
import com.steaminfosite.entity.type.GameDetailStatus;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Getter
@ToString
@NoArgsConstructor
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

    @Builder
    public SteamAppEntity(Long appId, String name, Long lastModified, Long priceChangeNumber, GameDetailStatus detailStatus) {
        this.appId = appId;
        this.name = name;
        this.lastModified = lastModified;
        this.priceChangeNumber = priceChangeNumber;
        this.detailStatus = detailStatus;
    }

    public void updateFromSteam(String name, Long lastModified, Long priceChangeNumber) {
        this.name = name;
        this.lastModified = lastModified;
        this.priceChangeNumber = priceChangeNumber;
    }

    public void updateDetailStatus(GameDetailStatus detailStatus) {
        this.detailStatus = detailStatus;
    }
}
