package com.gaiaproject.domain.entity.player;

import com.gaiaproject.domain.enumtype.booster.RoundBoosterType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "game_player_round_booster")
public class GamePlayerRoundBooster {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "round_booster_type", nullable = false, length = 50)
    private RoundBoosterType roundBoosterType;

    @Column(name = "selected_at", nullable = false)
    private LocalDateTime selectedAt;

    @Builder
    public GamePlayerRoundBooster(UUID gameId, UUID playerId, RoundBoosterType roundBoosterType) {
        this.gameId = gameId;
        this.playerId = playerId;
        this.roundBoosterType = roundBoosterType;
        this.selectedAt = LocalDateTime.now();
    }
}