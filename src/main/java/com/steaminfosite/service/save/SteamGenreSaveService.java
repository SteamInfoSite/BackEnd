package com.steaminfosite.service.save;

import com.steaminfosite.dto.api.depth1.SteamGenreList;

import java.util.List;

public interface SteamGenreSaveService {
    int saveGenres(List<SteamGenreList> genres);
}