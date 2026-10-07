package com.steaminfosite.repository;

import com.steaminfosite.entity.board.BoardCommentEntity;
import com.steaminfosite.entity.board.BoardPostEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardCommentRepository extends JpaRepository<BoardCommentEntity, Long> {

    Page<BoardCommentEntity> findByPost_Id(Long postId, Pageable pageable);

    @Modifying
    @Query("delete from BoardCommentEntity c where c.post = :post")
    int deleteByPost(@Param("post") BoardPostEntity post);
}
