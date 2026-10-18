package com.steaminfosite.service.board;

import com.steaminfosite.entity.board.BoardCommentEntity;
import com.steaminfosite.entity.board.BoardPostEntity;
import com.steaminfosite.entity.user.SiteUserEntity;
import com.steaminfosite.repository.BoardCommentRepository;
import com.steaminfosite.repository.BoardPostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardCommentService {

    private final BoardCommentRepository comments;
    private final BoardPostRepository posts;
    private final BoardCurrentUserService currentUser;

    public Page<BoardCommentEntity> list(Long postId, int page, int size) {
        validateId(postId, "게시글");
        validatePage(page, size);

        if (!posts.existsById(postId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "게시글이 없습니다");
        }

        PageRequest pageable = PageRequest.of(
                page - 1,
                size,
                Sort.by(
                        Sort.Order.asc("createdAt"),
                        Sort.Order.asc("id")
                )
        );
        return comments.findByPost_Id(postId, pageable);
    }

    @Transactional
    public BoardCommentEntity create(
            Long postId,
            String steamId,
            String content
    ) {
        SiteUserEntity author = currentUser.requireUser(steamId);
        BoardPostEntity post = findPost(postId);
        validateContent(content);

        return comments.save(BoardCommentEntity.create(
                post, author, content));
    }

    @Transactional
    public BoardCommentEntity update(
            Long commentId,
            String steamId,
            String content
    ) {
        SiteUserEntity user = currentUser.requireUser(steamId);
        BoardCommentEntity comment = findComment(commentId);
        requireAuthor(comment, user);
        validateContent(content);

        comment.update(content);
        return comment;
    }

    @Transactional
    public void delete(Long commentId, String steamId) {
        SiteUserEntity user = currentUser.requireUser(steamId);
        BoardCommentEntity comment = findComment(commentId);
        requireAuthor(comment, user);

        comments.delete(comment);
    }

    private BoardPostEntity findPost(Long postId) {
        validateId(postId, "게시글");
        return posts.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "게시글이 없습니다"));
    }

    private BoardCommentEntity findComment(Long commentId) {
        validateId(commentId, "댓글");
        return comments.findById(commentId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "댓글이 없습니다"));
    }

    private static void requireAuthor(
            BoardCommentEntity comment,
            SiteUserEntity user
    ) {
        if (!comment.getAuthor().getId().equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "댓글 작성자만 변경할 수 있습니다");
        }
    }

    private static void validateId(Long id, String name) {
        if (id == null || id < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, name + " 번호가 올바르지 않습니다");
        }
    }

    private static void validatePage(int page, int size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "페이지 값이 올바르지 않습니다");
        }
    }

    private static void validateContent(String content) {
        if (content == null || content.isBlank() || content.length() > 2000) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "댓글은 1~2000자여야 합니다");
        }
    }
}