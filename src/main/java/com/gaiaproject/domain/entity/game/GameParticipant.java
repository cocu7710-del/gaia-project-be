package com.gaiaproject.domain.entity.game;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 게임 입장 기록(좌석 선택 전 단계 포함).
 */
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Entity
@Table(name = "game_participant")
public class GameParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "game_id", nullable = false, updatable = false)
    private UUID gameId;

    @Column(name = "player_id", nullable = false, updatable = false)
    private UUID playerId;

    @Column(name = "entered_at", nullable = false)
    private LocalDateTime enteredAt;

    @Column(name = "claimed_seat_no")
    private Integer claimedSeatNo;

    /** 재입장 검증용 토큰 (브라우저 localStorage에 저장) */
    @Column(name = "rejoin_token", nullable = false, length = 32)
    private String rejoinToken;

    public static GameParticipant enter(UUID gameId, UUID playerId) {
        GameParticipant gp = new GameParticipant();
        gp.gameId = gameId;
        gp.playerId = playerId;
        gp.enteredAt = LocalDateTime.now();
        gp.rejoinToken = generateToken();
        return gp;
    }

    private static String generateToken() {
        // 8자리 랜덤 토큰 생성
        return UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }

    public void claimSeat(int seatNo) {
        this.claimedSeatNo = seatNo;
    }
}
