package com.gaiaproject.repository.player;

import com.gaiaproject.domain.entity.player.GamePlayerFleetProbe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GamePlayerFleetProbeRepository extends JpaRepository<GamePlayerFleetProbe, UUID> {

    List<GamePlayerFleetProbe> findByGameIdAndPlayerId(UUID gameId, UUID playerId);

    List<GamePlayerFleetProbe> findByGameIdOrderByPlacedAtAsc(UUID gameId);

    int countByGameIdAndPlayerId(UUID gameId, UUID playerId);

    int countByGameIdAndFleetName(UUID gameId, String fleetName);

    boolean existsByGameIdAndPlayerIdAndFleetName(UUID gameId, UUID playerId, String fleetName);

    void deleteByGameIdAndPlayerIdAndFleetName(UUID gameId, UUID playerId, String fleetName);

    void deleteByGameId(UUID gameId);
}
