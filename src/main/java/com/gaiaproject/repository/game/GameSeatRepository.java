package com.gaiaproject.repository.game;

import com.gaiaproject.domain.entity.game.GameSeat;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 좌석 조회/선점에 사용하는 Repository.
 */
public interface GameSeatRepository extends JpaRepository<GameSeat, UUID> {

    /** 게임의 모든 좌석 조회(렌더링/공개 상태용) */
    List<GameSeat> findByGameIdOrderBySeatNoAsc(UUID gameId);

    /**
     * 좌석 선점은 동시성 이슈가 있으니 비관적 락으로 row를 잠근다.
     * - 같은 seat을 동시에 클릭해도 1명만 성공하게 만들기 위함.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<GameSeat> findByGameIdAndSeatNo(UUID gameId, int seatNo);

    /** 좌석 번호로 정렬된 좌석 목록 조회 */
    List<GameSeat> findByGameIdOrderBySeatNo(UUID gameId);

    /** 플레이어가 배정된 좌석 조회 */
    Optional<GameSeat> findByGameIdAndPlayerId(UUID gameId, UUID playerId);

    /** 플레이어가 배정된 좌석 수 조회 */
    long countByGameIdAndPlayerIdIsNotNull(UUID gameId);
}
