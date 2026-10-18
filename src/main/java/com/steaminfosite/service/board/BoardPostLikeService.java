package com.steaminfosite.service.board;

import com.steaminfosite.dto.response.BoardPostLikeResponse;
import com.steaminfosite.entity.board.BoardPostEntity;
import com.steaminfosite.entity.board.BoardPostLikeEntity;
import com.steaminfosite.entity.user.SiteUserEntity;
import com.steaminfosite.repository.BoardPostLikeRepository;
import com.steaminfosite.repository.BoardPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardPostLikeService {
    private final BoardPostLikeRepository likes;
    private final BoardPostRepository posts;
    private final BoardCurrentUserService currentUser;

    public Map<Long, Long> likeCounts(List<Long> postIds) {
        if (postIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> counts = new HashMap<>();
        for (BoardPostLikeRepository.PostLikeCount row : likes.countByPostIds(postIds)) {
            counts.put(row.getPostId(), row.getLikeCount());
        }
        return counts;
    }

    public long likeCount(Long postId) {
        return likes.countByPost_Id(postId);
    }

    public boolean likedBy(Long postId, String steamId) {
        return steamId != null && !steamId.isBlank()
                && likes.existsByPost_IdAndUser_SteamId(postId, steamId);
    }

    @Transactional
    public BoardPostLikeResponse like(Long postId, String steamId) {
        SiteUserEntity user = currentUser.requireUser(steamId);
        if (postId == null || postId < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "게시글 번호가 올바르지 않습니다");
        }

        BoardPostEntity post = posts.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "게시글이 없습니다"));

        if (likes.existsByPost_IdAndUser_Id(postId, user.getId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 추천한 게시글입니다");
        }

        try {
            likes.saveAndFlush(BoardPostLikeEntity.create(post, user));
        } catch (DataIntegrityViolationException exception) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 추천한 게시글입니다");
        }

        return new BoardPostLikeResponse(likes.countByPost_Id(postId), true);
    }
}
