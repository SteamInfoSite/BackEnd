package com.steaminfosite.entity.id;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Getter
@Embeddable
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class GameGenreEntity {

    @Column(name = "genre_id")
    private String genreId;

    @Column(name = "genre_description")
    private String description;
}