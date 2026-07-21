package com.steaminfosite.entity.id;

import com.steaminfosite.entity.savetime.BaseTime;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Entity
@ToString
@NoArgsConstructor
public class GameInfoEntity extends BaseTime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long appId;

    @Column(nullable = false)
    private String gameName;

    @Column(nullable = false)
    private String type;

    @Builder
    public GameInfoEntity(Long appId, String gameName, String type) {
        this.appId = appId;
        this.gameName = gameName;
        this.type = type;
    }
}
