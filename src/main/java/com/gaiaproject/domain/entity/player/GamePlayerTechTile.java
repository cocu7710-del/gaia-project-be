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
@Table(name = "game_player_tech_tile")
public class GamePlayerTechTile {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    @Column(name = "tech_tile_code", nullable = false, length = 50)
    private String techTileCode;

    @Column(name = "is_covered", nullable = false)
    private Boolean isCovered = false;

    @Column(name = "covered_by", length = 50)
    private String coveredBy;

    /** ACTION 타입 타일: 이번 라운드 사용 여부 (라운드 시작 시 초기화) */
    @Column(name = "action_used")
    private Boolean actionUsed = false;

    @Column(name = "acquired_at", nullable = false)
    private LocalDateTime acquiredAt;

    @Column(name = "covered_at")
    private LocalDateTime coveredAt;

    @Builder
    public GamePlayerTechTile(UUID gameId, UUID playerId, String techTileCode) {
        this.gameId = gameId;
        this.playerId = playerId;
        this.techTileCode = techTileCode;
        this.isCovered = false;
        this.acquiredAt = LocalDateTime.now();
    }

    /** ACTION 타일 사용 여부 (null safe) */
    public boolean isActionUsed() {
        return Boolean.TRUE.equals(this.actionUsed);
    }

    /** ACTION 타일 사용 처리 */
    public void useAction() {
        this.actionUsed = true;
    }

    /** 라운드 시작 시 ACTION 타일 초기화 */
    public void resetAction() {
        this.actionUsed = false;
    }

    /**
     * 이 타일을 고급 타일로 덮기
     */
    public void cover(String advTileCode) {
        this.isCovered = true;
        this.coveredBy = advTileCode;
        this.coveredAt = LocalDateTime.now();
    }
}
