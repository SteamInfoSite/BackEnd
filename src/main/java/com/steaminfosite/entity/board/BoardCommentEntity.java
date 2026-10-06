package com.steaminfosite.entity.board;

import com.steaminfosite.entity.savetime.BaseTime;
import com.steaminfosite.entity.user.SiteUserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "board_comment")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardCommentEntity extends BaseTime {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    private BoardPostEntity post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "author_id", nullable = false)
    private SiteUserEntity author;

    @Column(nullable = false, length = 2000)
    private String content;

    public static BoardCommentEntity create(
            BoardPostEntity post,
            SiteUserEntity author,
            String content
    ) {
        BoardCommentEntity comment = new BoardCommentEntity();
        comment.post = Objects.requireNonNull(post);
        comment.author = Objects.requireNonNull(author);
        comment.content = Objects.requireNonNull(content);
        return comment;
    }

    public void update(String content) {
        this.content = Objects.requireNonNull(content);
    }
}
