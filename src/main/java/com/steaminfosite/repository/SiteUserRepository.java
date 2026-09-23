package com.steaminfosite.repository;

import com.steaminfosite.entity.user.SiteUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SiteUserRepository extends JpaRepository<SiteUserEntity, Long> {
    Optional<SiteUserEntity> findBySteamId(String steamId);
}