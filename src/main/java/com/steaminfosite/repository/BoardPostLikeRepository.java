package com.steaminfosite.repository;

import com.steaminfosite.entity.board.BoardPostEntity;
import com.steaminfosite.entity.board.BoardPostLikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface BoardPostLikeRepository extends JpaRepository<BoardPostLikeEntity, Long> {
    long countByPost_Id(Long postId);

    boolean existsByPost_IdAndUser_Id(Long postId, Long userId);

    boolean existsByPost_IdAndUser_SteamId(Long postId, String steamId);

    @Query("""
            select l.post.id as postId, count(l.id) as likeCount
            from BoardPostLikeEntity l
            where l.post.id in :postIds
            group by l.post.id
            """)
    List<PostLikeCount> countByPostIds(@Param("postIds") Collection<Long> postIds);

    interface PostLikeCount {
        Long getPostId();
        long getLikeCount();
    }

    @Modifying
    @Query("delete from BoardPostLikeEntity l where l.post = :post")
    int deleteByPost(@Param("post") BoardPostEntity post);
}
