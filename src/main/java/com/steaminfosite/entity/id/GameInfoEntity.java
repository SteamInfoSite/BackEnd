package com.steaminfosite.entity.id;

import com.steaminfosite.entity.savetime.BaseTime;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

    //출시일 원문을 기준으로 계산한 정렬용 날짜
    @Column
    private LocalDate releaseSortDate;

    //출시일 원문에 연 월 일이 모두 제공됐는지 여부
    @Column
    private Boolean releaseDateExact;

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
    @BatchSize(size = 100)
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
        updateCollectionIfChanged(this.developers, developers);
        updateCollectionIfChanged(this.publishers, publishers);
        updateCollectionIfChanged(this.genres, genres);
        updateCollectionIfChanged(this.categories, categories);
        updateCollectionIfChanged(this.movie, movie);
        this.recommendationsTotal = recommendationsTotal;
        this.website = website;
        this.windows = windows;
        this.mac = mac;
        this.linux = linux;
    }
    //목록 내용이 달라졌을 때만 기존 컬렉션을 수정한다
    private static <T> void updateCollectionIfChanged(
            List<T> current,
            List<T> incoming
    ) {
        //기존 동작과 동일하게 null은 빈 목록으로 취급한다
        List<T> newValues = incoming == null
                ? List.of()
                : new ArrayList<>(incoming);

        //순서는 무시하고 각 항목의 값과 등장 횟수를 비교한다
        if (current.size() == newValues.size()
                && countValues(current).equals(countValues(newValues))) {
            return;
        }

        current.clear();
        current.addAll(newValues);
    }

    // 각 값이 목록에 몇 번 들어 있는지 계산한다.
    private static <T> Map<T, Integer> countValues(List<T> values) {
        Map<T, Integer> counts = new HashMap<>();

        for (T value : values) {
            counts.merge(value, 1, Integer::sum);
        }

        return counts;
    }

    public void updateReleaseSortDate(LocalDate releaseSortDate) {
        this.releaseSortDate = releaseSortDate;
    }
    public void updateReleaseDateExact(boolean releaseDateExact) {
        this.releaseDateExact = releaseDateExact;
    }
}
