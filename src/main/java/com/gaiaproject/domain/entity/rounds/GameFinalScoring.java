package com.gaiaproject.domain.entity.rounds;

import com.gaiaproject.domain.enumtype.rounds.FinalScoringTileType;
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
@Table(name = "game_final_scoring")
public class GameFinalScoring {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Enumerated(EnumType.STRING)
    @Column(name = "scoring_tile_code", nullable = false, length = 50)
    private FinalScoringTileType scoringTileCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public GameFinalScoring(UUID gameId, Integer position, FinalScoringTileType scoringTileCode) {
        this.gameId = gameId;
        this.position = position;
        this.scoringTileCode = scoringTileCode;
        this.createdAt = LocalDateTime.now();
    }
}