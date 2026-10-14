package com.steaminfosite.entity.board;

import com.steaminfosite.entity.user.SiteUserEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
@Entity
@Table(
        name = "board_post_like",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_board_post_like_post_user",
                columnNames = {"post_id", "user_id"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BoardPostLikeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "post_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_board_post_like_post")
    )
    private BoardPostEntity post;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_board_post_like_user")
    )
    private SiteUserEntity user;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static BoardPostLikeEntity create(BoardPostEntity post, SiteUserEntity user) {
        BoardPostLikeEntity like = new BoardPostLikeEntity();
        like.post = Objects.requireNonNull(post);
        like.user = Objects.requireNonNull(user);
        return like;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
