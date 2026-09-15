package com.steaminfosite.repository;

import com.steaminfosite.entity.id.GameInfoEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface GameRepository extends JpaRepository<GameInfoEntity, Long> {
    Optional<GameInfoEntity> findByAppId(Long appId);

    //여러appId에 해당하는 기존상세정보를 한번의 IN쿼리로 조회
    List<GameInfoEntity> findAllByAppIdIn(Collection<Long> appIds);
}
