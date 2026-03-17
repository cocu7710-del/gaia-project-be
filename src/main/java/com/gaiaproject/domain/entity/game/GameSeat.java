package com.gaiaproject.domain.entity.game;

import com.gaiaproject.domain.enumtype.player.FactionType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 게임 생성 시 1~4 좌석이 미리 만들어지고,
 * 플레이어가 seat을 선택하면 playerId가 채워진다.
 */
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Entity
@Table(name = "game_seat")
public class GameSeat {

    /** 좌석 PK */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** 소속 게임(방) ID */
    @Column(name = "game_id", nullable = false, updatable = false)
    private UUID gameId;

    /** 실제 턴 순서 (1~4) */
    @Column(name = "seat_no", nullable = false)
    private int seatNo;

    /**
     * 다음 라운드의 선 플레이어 seatNo
     * (PASS 규칙에 의해 결정됨)
     */
    @Column(name = "turn_order", nullable = false)
    private int turnOrder;

    public void setTurnOrder(int order) { this.turnOrder = order; }

    /**
     * 좌석에 고정된 종족.
     * - DB에는 Enum 이름(String)으로 저장된다.
     * - Enum 순서가 바뀌어도 안전하게 유지하려면 STRING 저장이 필수.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "faction_type",nullable = false, length = 50)
    private FactionType factionType;

    /** 좌석을 선택한 플레이어 ID (없으면 null) */
    @Column(name = "player_id")
    private UUID playerId;

    /** 좌석 선택 시각 */
    @Column(name = "joined_at")
    private LocalDateTime joinedAt;

    /**
     * 좌석 생성 팩토리.
     *
     * @param gameId    방 ID
     * @param seatNo    좌석 번호(1~4)
     * @param turnOrder 턴 순서(1~4)
     * @param factionType      고정 종족
     */
    public static GameSeat create(UUID gameId, int seatNo, int turnOrder, FactionType factionType) {
        GameSeat s = new GameSeat();
        s.gameId = gameId;
        s.seatNo = seatNo;
        s.turnOrder = turnOrder;
        s.factionType = factionType;
        return s;
    }

    /**
     * 좌석 선점(Claim).
     * - 트랜잭션 내에서 호출되면 dirty checking으로 update 된다.
     */
    public void claim(UUID playerId) {
        this.playerId = playerId;
        this.joinedAt = LocalDateTime.now();
    }
}