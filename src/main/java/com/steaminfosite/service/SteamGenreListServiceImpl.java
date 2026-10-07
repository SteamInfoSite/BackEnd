package com.steaminfosite.service;

import com.steaminfosite.client.SteamApiClient;
import com.steaminfosite.dto.api.depth1.SteamGenreList;
import com.steaminfosite.repository.SteamGenreRepository;
import com.steaminfosite.service.save.SteamGenreSaveService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SteamGenreListServiceImpl
        implements SteamGenreListService {

    private final SteamApiClient steamApiClient;
    private final SteamGenreSaveService steamGenreSaveService;
    private final SteamGenreRepository steamGenreRepository;

    @Override
    public int collectSteamGenres() {
        List<SteamGenreList> genres = steamApiClient.fetchGenreList();

        return steamGenreSaveService.saveGenres(genres);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getGameGenreNames() {
        return steamGenreRepository.findGameGenreNames();
    }
}