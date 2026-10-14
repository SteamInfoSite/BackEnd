package com.steaminfosite.repository;

import com.steaminfosite.entity.board.BoardPostEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface BoardPostRepository extends JpaRepository<BoardPostEntity, Long> {

    @Modifying
    @Query("""
        update BoardPostEntity p
           set p.viewCount = p.viewCount + 1
         where p.id = :postId
        """)
    int increaseViewCount(@Param("postId") Long postId);

    @Query("""
        select p from BoardPostEntity p
        join fetch p.author
        where p.id = :postId
        """)
    Optional<BoardPostEntity> findByIdWithAuthor(@Param("postId") Long postId);

    @Override
    @EntityGraph(attributePaths = "author")
    Page<BoardPostEntity> findAll(Pageable pageable);
}