package com.gaiaproject.domain.entity.game;

import com.gaiaproject.domain.enumtype.action.ActionType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 게임 액션 기록
 * - FE에서 확정 후 BE에 저장되는 액션 기록
 * - 게임 리플레이 및 통계용
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "game_action")
public class GameAction {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "player_id", nullable = false)
    private UUID playerId;

    /** 디버깅용 비정규화 닉네임 — DB 트리거가 INSERT 시 자동 채움 */
    @Column(name = "nickname", length = 50, insertable = false, updatable = false)
    private String nickname;

    @Column(name = "round_number", nullable = false)
    private Integer roundNumber;

    @Column(name = "turn_sequence", nullable = false)
    private Integer turnSequence;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private ActionType actionType;

    /** 액션 상세 정보 (JSON) */
    @Column(name = "action_data", columnDefinition = "TEXT")
    private String actionData;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public GameAction(UUID gameId, UUID playerId, Integer roundNumber, Integer turnSequence,
                      ActionType actionType, String actionData) {
        this.gameId = gameId;
        this.playerId = playerId;
        this.roundNumber = roundNumber;
        this.turnSequence = turnSequence;
        this.actionType = actionType;
        this.actionData = actionData;
        this.createdAt = LocalDateTime.now();
    }
}
