package com.steaminfosite.repository;

import com.steaminfosite.entity.id.SteamAppEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SteamAppRepository extends JpaRepository<SteamAppEntity, Long> {
    Optional<SteamAppEntity> findByAppId(Long appId);
}
