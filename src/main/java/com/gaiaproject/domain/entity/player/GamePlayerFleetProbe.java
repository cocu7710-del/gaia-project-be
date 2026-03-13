package com.gaiaproject.domain.entity.player;

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
@Table(name = "game_player_fleet_probe")
public class GamePlayerFleetProbe {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "fleet_name", nullable = false, length = 30)
    private String fleetName;  // TF_MARS, ECLIPSE, TWILIGHT, REBELLION

    @Column(name = "placed_at", nullable = false)
    private LocalDateTime placedAt;

    @Builder
    public GamePlayerFleetProbe(UUID gameId, UUID playerId, String fleetName) {
        this.gameId = gameId;
        this.playerId = playerId;
        this.fleetName = fleetName;
        this.placedAt = LocalDateTime.now();
    }
}