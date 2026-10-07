package com.steaminfosite.entity.id;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "steam_genre")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SteamGenreEntity {
    @Id
    @Column(name = "genre_name", nullable = false, length = 100)
    private String genreName;

    public SteamGenreEntity(String genreName) {
        this.genreName = genreName;
    }
}