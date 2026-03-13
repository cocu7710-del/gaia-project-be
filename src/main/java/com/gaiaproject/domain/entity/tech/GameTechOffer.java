package com.gaiaproject.domain.entity.tech;

import com.gaiaproject.domain.enumtype.tech.TechTileCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "game_tech_offer")
public class GameTechOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Column(name = "tech_track", nullable = false)
    private String techTrack;

    @Enumerated(EnumType.STRING)
    @Column(name = "tech_tile_code", nullable = false, length = 50)
    private TechTileCode techTileCode;

    @Column(name = "taken_by_player_id")
    private UUID takenByPlayerId;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Builder
    public GameTechOffer(UUID gameId, Integer position, String techTrack, TechTileCode techTileCode) {
        this.gameId = gameId;
        this.position = position;
        this.techTrack = techTrack;
        this.techTileCode = techTileCode;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void take(UUID playerId) {
        this.takenByPlayerId = playerId;
        this.updatedAt = LocalDateTime.now();
    }
}