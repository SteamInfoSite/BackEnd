package com.steaminfosite.repository;

import com.steaminfosite.entity.price.GamePriceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface GamePriceRepository
        extends JpaRepository<GamePriceEntity, Long> {

    List<GamePriceEntity> findAllByAppIdIn(
            Collection<Long> appIds
    );
}