package com.steaminfosite.entity.id;

import com.steaminfosite.entity.savetime.BaseTime;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@ToString
@NoArgsConstructor
public class GameInfoEntity extends BaseTime {  //게임 상세 정보 저장
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long appId;

    @Column(nullable = false)
    private String gameName;

    @Column
    private String type;

    @Column
    private Long requiredAge;

    @Column
    private Boolean isFree;

    @Column(columnDefinition = "TEXT")
    private String shortDescription;    //짧은 게임 소개

    @Column(columnDefinition = "TEXT")
    private String headerImage;

    @Column(columnDefinition = "TEXT")
    private String capsuleImage;

    @Column
    private Boolean comingSoon;

    @Column
    private String relDate;


    @Builder
    public GameInfoEntity(Long appId, String gameName, String type, Long requiredAge, Boolean isFree, String shortDescription,
                          String headerImage, String capsuleImage, Boolean comingSoon, String relDate) {
        this.appId = appId;
        this.gameName = gameName;
        this.type = type;
        this.requiredAge = requiredAge;
        this.isFree = isFree;
        this.shortDescription = shortDescription;
        this.headerImage = headerImage;
        this.capsuleImage = capsuleImage;
        this.comingSoon = comingSoon;
        this.relDate = relDate;
    }
}
