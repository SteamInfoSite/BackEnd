package com.steaminfosite.repository;

import com.steaminfosite.entity.id.GameInfoEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    //게임 종류 무료 여부, 출시 예정 여부로 페이지 조회
    Page<GameInfoEntity> findByTypeAndIsFreeAndComingSoon(
            String type,
            Boolean isFree,
            Boolean comingSoon,
            Pageable pageable
    );
}
