package com.steaminfosite.dto.response;

import com.steaminfosite.entity.id.GameMovieEntity;

public record GameMovieResponse(
        Long movieId,
        String name,
        String thumbnail,
        String hlsH264
) {
    public static GameMovieResponse from(GameMovieEntity movie) {
        return new GameMovieResponse(
                movie.getMovieId(),
                movie.getName(),
                movie.getThumbnail(),
                movie.getHlsH264()
        );
    }
}