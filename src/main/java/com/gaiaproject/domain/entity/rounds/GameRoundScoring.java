package com.gaiaproject.domain.entity.rounds;

import com.gaiaproject.domain.enumtype.rounds.RoundScoringTileType;
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
@Table(name = "game_round_scoring")
public class GameRoundScoring {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "scoring_tile_code", nullable = false, length = 50)
    private RoundScoringTileType scoringTileCode;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public GameRoundScoring(UUID gameId, Integer roundNumber, RoundScoringTileType scoringTileCode) {
        this.gameId = gameId;
        this.roundNumber = roundNumber;
        this.scoringTileCode = scoringTileCode;
        this.createdAt = LocalDateTime.now();
    }
}