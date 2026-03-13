package com.gaiaproject.repository.game;

import com.gaiaproject.domain.entity.game.Game;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface GameRepository extends JpaRepository<Game, UUID> {

    /**
     * 가장 최근에 생성된 게임 1개 조회.
     * - 단일 게임 정책에서 "있으면 가져오고, 없으면 생성"에 사용
     */
    Optional<Game> findTopByOrderByCreatedAtDesc();

    /**
     * 중복 체크/조회용
     */
    boolean existsByRoomCode(String roomCode);

    /**
     * 방 코드로 게임 조회
     */
    Optional<Game> findByRoomCode(String roomCode);

}
