package com.steaminfosite.entity.id;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

@Getter
@Embeddable
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode
public class GameCategoryEntity {

    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "category_description")
    private String description;
}