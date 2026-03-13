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
@Table(name = "game_player_artifact")
public class GamePlayerArtifact {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "artifact_type", nullable = false, length = 50)
    private String artifactType;  // TODO: ArtifactType Enum 만들면 변경

    @Column(name = "acquired_at", nullable = false)
    private LocalDateTime acquiredAt;

    @Builder
    public GamePlayerArtifact(UUID gameId, UUID playerId, String artifactType) {
        this.gameId = gameId;
        this.playerId = playerId;
        this.artifactType = artifactType;
        this.acquiredAt = LocalDateTime.now();
    }
}