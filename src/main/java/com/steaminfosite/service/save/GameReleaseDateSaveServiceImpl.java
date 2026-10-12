package com.steaminfosite.service.save;

import com.steaminfosite.dto.response.GameReleaseDateTarget;
import com.steaminfosite.parser.SteamReleaseDateParser;
import com.steaminfosite.repository.GameRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GameReleaseDateSaveServiceImpl
        implements GameReleaseDateSaveService {

    private final GameRepository gameRepository;
    private final SteamReleaseDateParser steamReleaseDateParser;

    @Override
    @Transactional
    public int fillReleaseSortDates(
            List<GameReleaseDateTarget> targets
    ) {
        int updatedCount = 0;

        for (GameReleaseDateTarget target : targets) {
            LocalDate exactDate =
                    steamReleaseDateParser.parseExactDate(
                            target.relDate()
                    );

            LocalDate releaseSortDate = exactDate != null
                    ? exactDate
                    : steamReleaseDateParser.parseForSort(
                    target.relDate()
            );

            updatedCount +=
                    gameRepository.updateReleaseDateInfoIfMissing(
                            target.appId(),
                            target.relDate(),
                            releaseSortDate,
                            exactDate != null
                    );
        }

        return updatedCount;
    }
}