package com.steaminfosite.entity.id;

import com.steaminfosite.entity.savetime.BaseTime;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

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

    @Column(nullable = false, columnDefinition = "TEXT")
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

    @Column(columnDefinition = "TEXT")
    private String website;

    @Column(columnDefinition = "TEXT")
    private String supportedLanguages;

    @ToString.Exclude
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(   //db에 별도 테이블이 생김
            name = "game_info_developer",
            joinColumns = @JoinColumn(name = "game_info_id")
    )
    @Column(columnDefinition = "TEXT")
    private List<String> developers = new ArrayList<>();

    @ToString.Exclude
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "game_info_publisher",
            joinColumns = @JoinColumn(name = "game_info_id")
    )
    @Column(columnDefinition = "TEXT")
    private List<String> publishers = new ArrayList<>();

    @ToString.Exclude
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "game_info_genres",
            joinColumns = @JoinColumn(name = "game_info_id")
    )
    private List<GameGenreEntity> genres = new ArrayList<>();

    @ToString.Exclude
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "game_info_categories",
            joinColumns = @JoinColumn(name = "game_info_id")
    )
    private List<GameCategoryEntity> categories = new ArrayList<>();

    @ToString.Exclude
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "game_info_movie",
            joinColumns = @JoinColumn(name = "game_info_id")
    )
    private List<GameMovieEntity> movie = new ArrayList<>();

    @Column
    private Long recommendationsTotal;

    @Column
    private Boolean windows;

    @Column
    private Boolean mac;

    @Column
    private Boolean linux;

    @Builder
    public GameInfoEntity(Long appId, String gameName, String type, Long requiredAge, Boolean isFree, String shortDescription,
                          String headerImage, String capsuleImage, Boolean comingSoon, String relDate, String supportedLanguages,
                          List<String> developers, List<String> publishers, List<GameGenreEntity> genres, List<GameCategoryEntity> categories,
                          List<GameMovieEntity> movie, Long recommendationsTotal, String website, Boolean windows, Boolean mac, Boolean linux) {
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
        this.supportedLanguages = supportedLanguages;
        if(developers != null) {
            this.developers.addAll(developers);
        }
        if(publishers != null) {
            this.publishers.addAll(publishers);
        }
        if(genres != null){
            this.genres.addAll(genres);
        }
        if(categories != null){
            this.categories.addAll(categories);
        }
        if(movie != null){
            this.movie.addAll(movie);
        }
        this.recommendationsTotal = recommendationsTotal;
        this.website = website;
        this.windows = windows;
        this.mac = mac;
        this.linux = linux;
    }

    public void updateFromSteam(String gameName, String type, Long requiredAge, Boolean isFree,
                                String shortDescription, String headerImage, String capsuleImage,
                                Boolean comingSoon, String relDate, String supportedLanguages,
                                List<String> developers, List<String> publishers, List<GameGenreEntity> genres,
                                List<GameCategoryEntity> categories, List<GameMovieEntity> movie, Long recommendationsTotal,
                                String website, Boolean windows, Boolean mac, Boolean linux) {
        this.gameName = gameName;
        this.type = type;
        this.requiredAge = requiredAge;
        this.isFree = isFree;
        this.shortDescription = shortDescription;
        this.headerImage = headerImage;
        this.capsuleImage = capsuleImage;
        this.comingSoon = comingSoon;
        this.relDate = relDate;
        this.supportedLanguages = supportedLanguages;
        this.developers.clear();    //업데이트 시 지우고 새로 받은 목록으로 교체
        if (developers != null) {
            this.developers.addAll(developers);
        }
        this.publishers.clear();
        if (publishers != null) {
            this.publishers.addAll(publishers);
        }
        this.genres.clear();
        if(genres != null){
            this.genres.addAll(genres);
        }
        this.categories.clear();
        if(categories != null){
            this.categories.addAll(categories);
        }
        this.movie.clear();
        if(movie != null){
            this.movie.addAll(movie);
        }
        this.recommendationsTotal = recommendationsTotal;
        this.website = website;
        this.windows = windows;
        this.mac = mac;
        this.linux = linux;
    }
}
