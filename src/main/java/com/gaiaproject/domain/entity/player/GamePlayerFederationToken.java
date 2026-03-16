package com.gaiaproject.domain.entity.player;

import com.gaiaproject.domain.enumtype.federation.FederationTileType;
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
@Table(name = "game_player_federation_token")
public class GamePlayerFederationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "federation_tile_type", nullable = false, length = 50)
    private FederationTileType federationTileType;

    @Column(name = "acquired_at", nullable = false)
    private LocalDateTime acquiredAt;

    @Column(name = "used", nullable = false)
    private boolean used = false;

    @Builder
    public GamePlayerFederationToken(UUID gameId, UUID playerId, FederationTileType federationTileType) {
        this.gameId = gameId;
        this.playerId = playerId;
        this.federationTileType = federationTileType;
        this.acquiredAt = LocalDateTime.now();
        this.used = false;
    }

    /** 사용 가능한 토큰 뒤집기 */
    public void markUsed() { this.used = true; }
}