package com.steaminfosite.repository;

import com.steaminfosite.entity.id.GameInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GameRepository extends JpaRepository<GameInfoEntity, Long> {
}
