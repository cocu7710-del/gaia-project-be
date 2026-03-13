package com.gaiaproject.domain.entity.booster;

import com.gaiaproject.domain.entity.game.Game;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 한 게임에서 공개된 라운드 부스터 카드(오퍼) 1장을 나타내는 엔티티.
 * - 게임 시작 시 (players + 3)장 생성
 * - position으로 공개 순서를 유지
 * - pickedBySeatNo가 null이면 아직 선택되지 않음
 */
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Entity
@Table(
        name = "game_booster_offer",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_game_booster_offer_game_pos", columnNames = {"game_id", "position"}),
                @UniqueConstraint(name = "uk_game_booster_offer_game_code", columnNames = {"game_id", "booster_code"})
        },
        indexes = {
                @Index(name = "ix_game_booster_offer_game", columnList = "game_id")
        }
)
public class GameBoosterOffer {

    /** PK */
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** 소속 게임 */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "game_id", nullable = false)
    private Game game;

    /** 부스터 코드 (예: RB01, RB02...) */
    @Column(name = "booster_code", nullable = false, length = 20)
    private String boosterCode;

    /** 공개 순서(0..n-1) */
    @Column(nullable = false)
    private int position;

    /** 선택한 좌석 번호(1~4). 미선택이면 null */
    @Column(name = "picked_by_seat_no")
    private Integer pickedBySeatNo;

    /** 생성 시각 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** 수정 시각 */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 낙관적 락 버전 */
    @Version
    @Column(nullable = false)
    private long version;

    /** 부스터를 가져간 플레이어 ID (교체 시스템용) */
    @Column(name = "taken_by_player_id")
    private UUID takenByPlayerId;

    /**
     * 생성 팩토리.
     * - 게임 세팅 시 position 순서대로 생성
     */
    public static GameBoosterOffer create(Game game, String boosterCode, int position) {
        GameBoosterOffer o = new GameBoosterOffer();
        o.game = game;
        o.boosterCode = boosterCode;
        o.position = position;
        o.pickedBySeatNo = null;
        o.createdAt = LocalDateTime.now();
        o.updatedAt = LocalDateTime.now();
        return o;
    }

    /** 아직 선택 가능한지 */
    public boolean isAvailable() {
        return this.pickedBySeatNo == null;
    }

    /** 부스터 선택 */
    public void pick(int seatNo) {
        if (seatNo < 1 || seatNo > 4) {
            throw new IllegalArgumentException("seatNo must be between 1 and 4");
        }
        if (this.pickedBySeatNo != null) {
            throw new IllegalStateException("already picked");
        }
        this.pickedBySeatNo = seatNo;
        this.updatedAt = LocalDateTime.now();
    }

    /** 부스터를 풀에 반환 (교체 시) */
    public void returnToPool() {
        this.pickedBySeatNo = null;
        this.takenByPlayerId = null;
        this.updatedAt = LocalDateTime.now();
    }

    /** 플레이어가 부스터를 가져감 (교체 시) */
    public void takeByPlayer(UUID playerId) {
        if (this.takenByPlayerId != null) {
            throw new IllegalStateException("already taken by another player");
        }
        this.takenByPlayerId = playerId;
        this.updatedAt = LocalDateTime.now();
    }
}
