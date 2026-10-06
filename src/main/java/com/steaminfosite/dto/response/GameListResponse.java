package com.steaminfosite.dto.response;

import com.steaminfosite.dto.response.GameInfoResponse.Platforms;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@AllArgsConstructor
public class GameListResponse {

    private final List<Item> items;
    private final long totalCount;
    private final int page;
    private final int pageSize;

    @Getter
    @Builder
    public static class Item {

        private Long appId;
        private String gameName;

        private String headerImage;
        private String capsuleImage;

        private Boolean isFree;
        private Boolean comingSoon;
        private String relDate;

        private List<String> genres;
        private Platforms platforms;

        private Long recommendationsTotal;
        private LocalDateTime updatedAt;
    }
}