package com.steaminfosite.entity.board;

import com.steaminfosite.entity.savetime.BaseTime;
import com.steaminfosite.entity.type.BoardCategory;
import com.steaminfosite.entity.user.SiteUserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

@Getter
@Entity
@Table(name = "board_post")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardPostEntity extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private SiteUserEntity author;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private BoardCategory category;

    @Column(nullable = false, length = 200)
    private String title;

    // Service에서 정제한 HTML만 저장
    @Column(name = "content_html", nullable = false, columnDefinition = "TEXT")
    private String contentHtml;

    // Service에서 contentHtml로부터 추출한 일반 텍스트
    @Column(name = "content_text", nullable = false, columnDefinition = "TEXT")
    private String contentText;

    @Column(name = "view_count", nullable = false)
    private long viewCount;

    public static BoardPostEntity create(
            SiteUserEntity author,
            BoardCategory category,
            String title,
            String contentHtml,
            String contentText
    ) {
        BoardPostEntity post = new BoardPostEntity();
        post.author = Objects.requireNonNull(author);
        post.category = Objects.requireNonNull(category);
        post.title = Objects.requireNonNull(title);
        post.contentHtml = Objects.requireNonNull(contentHtml);
        post.contentText = Objects.requireNonNull(contentText);
        post.viewCount = 0;
        return post;
    }

    public void update(
            BoardCategory category,
            String title,
            String contentHtml,
            String contentText
    ) {
        this.category = Objects.requireNonNull(category);
        this.title = Objects.requireNonNull(title);
        this.contentHtml = Objects.requireNonNull(contentHtml);
        this.contentText = Objects.requireNonNull(contentText);
    }
}
