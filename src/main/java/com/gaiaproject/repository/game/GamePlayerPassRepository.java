package com.gaiaproject.repository.game;

import com.gaiaproject.domain.entity.game.GamePlayerPass;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GamePlayerPassRepository extends JpaRepository<GamePlayerPass, UUID> {

    /**
     * 특정 게임의 특정 라운드에 패스한 플레이어 수 조회
     */
    long countByGameIdAndRoundNumber(UUID gameId, Integer roundNumber);

    /**
     * 특정 플레이어가 특정 라운드에 패스했는지 확인
     */
    boolean existsByGameIdAndPlayerIdAndRoundNumber(UUID gameId, UUID playerId, Integer roundNumber);

    /**
     * 특정 게임의 특정 라운드 패스 기록 조회
     */
    List<GamePlayerPass> findByGameIdAndRoundNumber(UUID gameId, Integer roundNumber);

    /**
     * 특정 플레이어의 패스 기록 조회
     */
    List<GamePlayerPass> findByGameIdAndPlayerId(UUID gameId, UUID playerId);
}
