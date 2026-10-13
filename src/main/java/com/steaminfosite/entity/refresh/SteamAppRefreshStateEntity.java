package com.steaminfosite.entity.refresh;

import com.steaminfosite.entity.savetime.BaseTime;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "steam_app_refresh_state")
public class SteamAppRefreshStateEntity extends BaseTime {

    public static final Long APP_LIST_STATE_ID = 1L;

    @Id
    private Long id;

    @Column(name = "last_successful_started_at")
    private Instant lastSuccessfulStartedAt;

    @Version
    private Long version;

    public static SteamAppRefreshStateEntity createInitialState() {
        SteamAppRefreshStateEntity state = new SteamAppRefreshStateEntity();

        state.id = APP_LIST_STATE_ID;
        return state;
    }

    public void markCompleted(Instant startedAt) {
        this.lastSuccessfulStartedAt = Objects.requireNonNull(startedAt);
    }
}