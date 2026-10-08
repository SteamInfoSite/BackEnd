package com.steaminfosite.service.save;

import com.steaminfosite.dto.api.depth1.SteamGenreList;
import com.steaminfosite.entity.id.SteamGenreEntity;
import com.steaminfosite.repository.GameRepository;
import com.steaminfosite.repository.SteamGenreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SteamGenreSaveServiceImpl
        implements SteamGenreSaveService {

    private final SteamGenreRepository steamGenreRepository;
    private final GameRepository gameRepository;

    @Override
    @Transactional
    public int saveGenres(List<SteamGenreList> genres) {
        if (genres == null || genres.isEmpty()) {
            throw new IllegalArgumentException(
                    "저장할 장르 목록이 없습니다"
            );
        }

        Map<String, String> genreNames = new LinkedHashMap<>();
        //스팀 목록 API에서 받은 장르명 추가
        for (SteamGenreList genre : genres) {
            if (genre != null) {
                addGenreName(genreNames, genre.getId());
            }
        }
        // 기존 게임 상세에 저장된 장르명으로 목록 보완
        for (String name : gameRepository.findGameGenreNames()) {
            addGenreName(genreNames, name);
        }

        if (genreNames.isEmpty()) {
            throw new IllegalArgumentException(
                    "저장 가능한 장르명이 없습니다"
            );
        }
        //이미 저장된 이름도 대소문자를 무시해서 비교
        Set<String> savedNames = steamGenreRepository.findAll()
                .stream()
                .map(SteamGenreEntity::getGenreName)
                .map(name -> name.trim().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
        //아직 저장되지 않은 이름만 저장 대상으로 선정
        List<SteamGenreEntity> saveTargets =
                genreNames.entrySet().stream()
                        .filter(entry ->
                                !savedNames.contains(entry.getKey()))
                        .map(entry ->
                                new SteamGenreEntity(entry.getValue()))
                        .toList();
        if (!saveTargets.isEmpty()) {
            steamGenreRepository.saveAll(saveTargets);
        }
        //이번에 합쳐서 처리한 전체 장르명 개수
        return genreNames.size();
    }

    private void addGenreName(
            Map<String, String> genreNames,
            String name
    ) {
        if (name == null || name.isBlank()) {
            return;
        }
        String cleanedName = name.trim();

        genreNames.putIfAbsent(
                cleanedName.toLowerCase(Locale.ROOT),
                cleanedName
        );
    }
}