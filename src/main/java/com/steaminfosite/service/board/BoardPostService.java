package com.steaminfosite.service.board;

import com.steaminfosite.entity.board.BoardPostEntity;
import com.steaminfosite.entity.type.BoardCategory;
import com.steaminfosite.entity.type.SiteUserRole;
import com.steaminfosite.entity.user.SiteUserEntity;
import com.steaminfosite.repository.BoardCommentRepository;
import com.steaminfosite.repository.BoardPostLikeRepository;
import com.steaminfosite.repository.BoardPostRepository;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.safety.Safelist;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardPostService {

    private static final int MAX_HTML_BYTES = 3 * 1024 * 1024;
    private static final int MAX_TEXT_LENGTH = 20_000;

    private static final Safelist CONTENT_POLICY = new Safelist()
            .addTags(
                    "p", "br", "b", "strong", "i", "em", "u",
                    "ul", "ol", "li", "blockquote", "pre", "code",
                    "span", "a"
            )
            .addAttributes("span", "style")
            .addAttributes("a", "href")
            .addProtocols("a", "href", "https")
            .addEnforcedAttribute("a", "target", "_blank")
            .addEnforcedAttribute("a", "rel", "noopener noreferrer");

    private final BoardPostRepository posts;
    private final BoardCommentRepository comments;
    private final BoardPostLikeRepository likes;
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

    public Map<Long, Long> commentCounts(List<Long> postIds) {
        if (postIds.isEmpty()) {
            return Map.of();
        }

        Map<Long, Long> counts = new HashMap<>();
        for (BoardCommentRepository.PostCommentCount row
                : comments.countByPostIds(postIds)) {
            counts.put(row.getPostId(), row.getCommentCount());
        }
        return counts;
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

        return posts.findByIdWithAuthor(postId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "게시글이 없습니다"));
    }

    @Transactional
    public BoardPostEntity create(
            String steamId,
            BoardCategory category,
            String title,
            String contentHtml
    ) {
        SiteUserEntity author = currentUser.requireUser(steamId);
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
            String steamId,
            BoardCategory category,
            String title,
            String contentHtml
    ) {
        SiteUserEntity user = currentUser.requireUser(steamId);
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
    public void delete(Long postId, String steamId) {
        SiteUserEntity user = currentUser.requireUser(steamId);
        BoardPostEntity post = findPost(postId);

        requireAuthor(post, user);

        likes.deleteByPost(post);
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

        Document document = Jsoup.parseBodyFragment(rawHtml);

        if (!document.select(
                "img, picture, source, svg, video, iframe, object, embed"
        ).isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "이미지와 동영상은 아직 지원하지 않습니다");
        }

        normalizeTextStyles(document);

        String safeHtml = Jsoup.clean(document.body().html(), CONTENT_POLICY);
        String text = Jsoup.parseBodyFragment(safeHtml).text();

        if (text.replace('\u00A0', ' ').isBlank()
                || text.codePointCount(0, text.length()) > MAX_TEXT_LENGTH
                || safeHtml.getBytes(StandardCharsets.UTF_8).length > MAX_HTML_BYTES) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "본문은 1~20000자여야 합니다");
        }

        return new CleanContent(safeHtml, text);
    }

    private static void normalizeTextStyles(Document document) {
        for (Element span : document.select("span")) {
            String fontFamily = null;
            String fontSize = null;
            String textColor = null;
            String highlightColor = null;

            for (String part : span.attr("style").split(";")) {
                String[] declaration = part.split(":", 2);
                if (declaration.length != 2) {
                    continue;
                }

                String property = declaration[0].trim();
                String value = declaration[1].trim();

                if (property.equalsIgnoreCase("font-family")) {
                    fontFamily = allowedFontFamily(value);
                } else if (property.equalsIgnoreCase("font-size")
                        && value.matches("(10|13|16|18|24|32|48)px")) {
                    fontSize = value;
                } else if (property.equalsIgnoreCase("color")) {
                    textColor = allowedTextColor(value);
                } else if (property.equalsIgnoreCase("background-color")) {
                    highlightColor = allowedHighlightColor(value);
                }
            }

            span.removeAttr("style");

            StringBuilder safeStyle = new StringBuilder();
            if (fontFamily != null) {
                safeStyle.append("font-family: ").append(fontFamily);
            }
            if (fontSize != null) {
                if (safeStyle.length() > 0) {
                    safeStyle.append("; ");
                }
                safeStyle.append("font-size: ").append(fontSize);
            }
            if (textColor != null) {
                if (safeStyle.length() > 0) {
                    safeStyle.append("; ");
                }
                safeStyle.append("color: ").append(textColor);
            }
            if (highlightColor != null) {
                if (safeStyle.length() > 0) {
                    safeStyle.append("; ");
                }
                safeStyle.append("background-color: ").append(highlightColor);
            }
            if (safeStyle.length() > 0) {
                span.attr("style", safeStyle.toString());
            }
        }
    }

    private static String allowedFontFamily(String value) {
        String family = value.trim();

        if (family.length() >= 2
                && ((family.startsWith("\"") && family.endsWith("\""))
                || (family.startsWith("'") && family.endsWith("'")))) {
            family = family.substring(1, family.length() - 1);
        }

        return switch (family) {
            case "Malgun Gothic" -> "'Malgun Gothic'";
            case "Arial", "Georgia" -> family;
            default -> null;
        };
    }

    private static String allowedTextColor(String value) {
        String color = value.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);

        return switch (color) {
            case "#222222", "rgb(34,34,34)" -> "#222222";
            case "#dc2626", "rgb(220,38,38)" -> "#dc2626";
            case "#ea580c", "rgb(234,88,12)" -> "#ea580c";
            case "#15803d", "rgb(21,128,61)" -> "#15803d";
            case "#2563eb", "rgb(37,99,235)" -> "#2563eb";
            case "#7c3aed", "rgb(124,58,237)" -> "#7c3aed";
            default -> null;
        };
    }

    private static String allowedHighlightColor(String value) {
        String color = value.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);

        return switch (color) {
            case "#fef08a", "rgb(254,240,138)" -> "#fef08a";
            case "#fecdd3", "rgb(254,205,211)" -> "#fecdd3";
            case "#bbf7d0", "rgb(187,247,208)" -> "#bbf7d0";
            case "#bfdbfe", "rgb(191,219,254)" -> "#bfdbfe";
            case "#e9d5ff", "rgb(233,213,255)" -> "#e9d5ff";
            default -> null;
        };
    }

    private record CleanContent(String html, String text) {
    }
}