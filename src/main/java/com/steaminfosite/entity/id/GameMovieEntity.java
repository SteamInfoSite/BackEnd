package com.steaminfosite.entity.id;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Embeddable
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GameMovieEntity {

    @Column(name = "movie_id")
    private Long movieId;

    @Column(name = "movie_name")
    private String name;

    @Column(name = "thumbnail", columnDefinition = "TEXT")
    private String thumbnail;

    @Column(name = "hls_h264", columnDefinition = "TEXT")
    private String hlsH264;

    @Column(name = "highlight")
    private Boolean highlight;
}
