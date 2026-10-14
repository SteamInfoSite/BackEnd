package com.steaminfosite.repository;

import java.util.Collection;
import java.util.List;
import com.steaminfosite.entity.board.BoardCommentEntity;
import com.steaminfosite.entity.board.BoardPostEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BoardCommentRepository extends JpaRepository<BoardCommentEntity, Long> {

    @EntityGraph(attributePaths = "author")
    Page<BoardCommentEntity> findByPost_Id(Long postId, Pageable pageable);

    @Query("""
    select c.post.id as postId, count(c.id) as commentCount
    from BoardCommentEntity c
    where c.post.id in :postIds
    group by c.post.id
    """)
    List<PostCommentCount> countByPostIds(@Param("postIds") Collection<Long> postIds);

    interface PostCommentCount {
        Long getPostId();
        long getCommentCount();
    }

    @Modifying
    @Query("delete from BoardCommentEntity c where c.post = :post")
    int deleteByPost(@Param("post") BoardPostEntity post);
}
