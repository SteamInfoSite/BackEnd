package com.steaminfosite.repository;

import com.steaminfosite.entity.board.BoardPostEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardPostRepository extends JpaRepository<BoardPostEntity, Long> {

    @Modifying
    @Query("""
        update BoardPostEntity p
           set p.viewCount = p.viewCount + 1
         where p.id = :postId
        """)
    int increaseViewCount(@Param("postId") Long postId);
}