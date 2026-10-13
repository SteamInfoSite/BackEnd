package com.steaminfosite.service.save;

import com.steaminfosite.dto.response.GameReleaseDateTarget;

import java.util.List;

public interface GameReleaseDateSaveService {
    int fillReleaseSortDates(List<GameReleaseDateTarget> targets);
}