package com.gaiaproject.domain.entity.game;

import com.gaiaproject.domain.enumtype.action.PowerActionType;
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
@Table(name = "game_power_action_usage")
public class GamePowerActionUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "power_action_type", nullable = false, length = 100)
    private PowerActionType powerActionType;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "used_at", nullable = false)
    private LocalDateTime usedAt;

    @Builder
    public GamePowerActionUsage(UUID gameId, Integer roundNumber, PowerActionType powerActionType, UUID playerId) {
        this.gameId = gameId;
        this.roundNumber = roundNumber;
        this.powerActionType = powerActionType;
        this.playerId = playerId;
        this.usedAt = LocalDateTime.now();
    }
}