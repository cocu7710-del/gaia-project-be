package com.gaiaproject.repository.player;

import com.gaiaproject.domain.entity.player.GamePlayerState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GamePlayerStateRepository extends JpaRepository<GamePlayerState, UUID> {

    Optional<GamePlayerState> findByGameIdAndPlayerId(UUID gameId, UUID playerId);

    List<GamePlayerState> findByGameId(UUID gameId);

    Optional<GamePlayerState> findByGameIdAndSeatNo(UUID gameId, Integer seatNo);

    boolean existsByGameId(UUID gameId);
}