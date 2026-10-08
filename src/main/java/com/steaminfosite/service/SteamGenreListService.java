package com.steaminfosite.service;

import java.util.List;

public interface SteamGenreListService {
    int collectSteamGenres();
    List<String> getGameGenreNames();
}