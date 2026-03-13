package com.gaiaproject.domain.entity.game;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 게임 플레이어 패스 기록
 * - 라운드당 1회만 패스 가능 (UNIQUE 제약)
 * - 패스 시 다음 라운드 부스터를 선택함
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "game_player_pass",
       uniqueConstraints = @UniqueConstraint(columnNames = {"game_id", "player_id", "round_number"}))
public class GamePlayerPass {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;

    @Column(name = "passed_at", nullable = false)
    private LocalDateTime passedAt;

    @Builder
    public GamePlayerPass(UUID gameId, UUID playerId, Integer roundNumber) {
        this.gameId = gameId;
        this.playerId = playerId;
        this.roundNumber = roundNumber;
        this.passedAt = LocalDateTime.now();
    }
}
