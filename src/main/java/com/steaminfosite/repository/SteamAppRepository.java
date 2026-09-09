package com.steaminfosite.repository;

import com.steaminfosite.entity.id.SteamAppEntity;
import com.steaminfosite.entity.type.GameDetailStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SteamAppRepository extends JpaRepository<SteamAppEntity, Long> {
    Optional<SteamAppEntity> findByAppId(Long appId);

    //DetailStatus에서 여러해당상태의 appid기준으로 오름차순 정렬
    List<SteamAppEntity> findByDetailStatusInOrderByAppIdAsc(
            List<GameDetailStatus> detailStatus,
            Pageable pageable);
}
