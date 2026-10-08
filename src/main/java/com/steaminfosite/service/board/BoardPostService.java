package com.steaminfosite.service.board;

import com.steaminfosite.entity.board.BoardPostEntity;
import com.steaminfosite.entity.type.BoardCategory;
import com.steaminfosite.entity.type.SiteUserRole;
import com.steaminfosite.entity.user.SiteUserEntity;
import com.steaminfosite.repository.BoardCommentRepository;
import com.steaminfosite.repository.BoardPostRepository;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardPostService {

    private static final int MAX_HTML_BYTES = 3 * 1024 * 1024;
    private static final int MAX_TEXT_LENGTH = 20_000;

    private static final Safelist CONTENT_POLICY = new Safelist()
            .addTags(
                    "p", "br", "b", "strong", "i", "em", "u",
                    "ul", "ol", "li", "blockquote", "pre", "code"
            );

    private final BoardPostRepository posts;
    private final BoardCommentRepository comments;
    private final BoardCurrentUserService currentUser;

    public Page<BoardPostEntity> list(int page, int size) {
        if (page < 1 || size < 1 || size > 100) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "페이지 값이 올바르지 않습니다");
        }

        PageRequest pageable = PageRequest.of(
                page - 1,
                size,
                Sort.by(
                        Sort.Order.desc("createdAt"),
                        Sort.Order.desc("id")
                )
        );
        return posts.findAll(pageable);
    }

    @Transactional
    public BoardPostEntity detail(Long postId) {
        if (postId == null || postId < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "게시글 번호가 올바르지 않습니다");
        }

        if (posts.increaseViewCount(postId) == 0) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "게시글이 없습니다");
        }

        return posts.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "게시글이 없습니다"));
    }

    @Transactional
    public BoardPostEntity create(
            String sessionSteamId,
            BoardCategory category,
            String title,
            String contentHtml
    ) {
        SiteUserEntity author = currentUser.requireUser(sessionSteamId);
        validateCategory(category, author);

        String safeTitle = validateTitle(title);
        CleanContent content = cleanContent(contentHtml);

        return posts.save(BoardPostEntity.create(
                author,
                category,
                safeTitle,
                content.html(),
                content.text()
        ));
    }

    @Transactional
    public BoardPostEntity update(
            Long postId,
            String sessionSteamId,
            BoardCategory category,
            String title,
            String contentHtml
    ) {
        SiteUserEntity user = currentUser.requireUser(sessionSteamId);
        BoardPostEntity post = findPost(postId);

        requireAuthor(post, user);
        validateCategory(category, user);

        String safeTitle = validateTitle(title);
        CleanContent content = cleanContent(contentHtml);

        post.update(
                category,
                safeTitle,
                content.html(),
                content.text()
        );
        return post;
    }

    @Transactional
    public void delete(Long postId, String sessionSteamId) {
        SiteUserEntity user = currentUser.requireUser(sessionSteamId);
        BoardPostEntity post = findPost(postId);

        requireAuthor(post, user);

        comments.deleteByPost(post);
        posts.delete(post);
    }

    private BoardPostEntity findPost(Long postId) {
        if (postId == null || postId < 1) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "게시글 번호가 올바르지 않습니다");
        }

        return posts.findById(postId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "게시글이 없습니다"));
    }

    private static void requireAuthor(
            BoardPostEntity post,
            SiteUserEntity user
    ) {
        if (!post.getAuthor().getId().equals(user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "게시글 작성자만 변경할 수 있습니다");
        }
    }

    private static void validateCategory(
            BoardCategory category,
            SiteUserEntity user
    ) {
        if (category == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "말머리가 필요합니다");
        }

        if (category == BoardCategory.NOTICE
                && user.getRole() != SiteUserRole.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN, "공지 작성 권한이 없습니다");
        }
    }

    private static String validateTitle(String title) {
        if (title == null || title.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "제목이 필요합니다");
        }

        String safeTitle = title.strip();
        if (safeTitle.length() > 200) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "제목은 200자 이하여야 합니다");
        }
        return safeTitle;
    }

    private static CleanContent cleanContent(String rawHtml) {
        if (rawHtml == null
                || rawHtml.getBytes(StandardCharsets.UTF_8).length > MAX_HTML_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "본문 HTML 크기가 올바르지 않습니다");
        }

        if (!Jsoup.parseBodyFragment(rawHtml)
                .select("img, picture, source, svg, video, iframe, object, embed")
                .isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "이미지와 동영상은 아직 지원하지 않습니다");
        }

        String safeHtml = Jsoup.clean(rawHtml, CONTENT_POLICY);
        String text = Jsoup.parseBodyFragment(safeHtml).text();

        if (text.replace('\u00A0', ' ').isBlank()
                || text.codePointCount(0, text.length()) > MAX_TEXT_LENGTH
                || safeHtml.getBytes(StandardCharsets.UTF_8).length > MAX_HTML_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "본문은 1~20000자여야 합니다");
        }

        return new CleanContent(safeHtml, text);
    }

    private record CleanContent(String html, String text) {
    }
}