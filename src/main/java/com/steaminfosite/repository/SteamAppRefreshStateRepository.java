package com.steaminfosite.repository;

import com.steaminfosite.entity.refresh.SteamAppRefreshStateEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SteamAppRefreshStateRepository
        extends JpaRepository<SteamAppRefreshStateEntity, Long> {
}