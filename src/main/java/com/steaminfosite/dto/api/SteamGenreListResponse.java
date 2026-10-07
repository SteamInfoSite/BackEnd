package com.steaminfosite.dto.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.steaminfosite.dto.api.depth1.SteamGenreList;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SteamGenreListResponse {
    private List<SteamGenreList> genres;
}