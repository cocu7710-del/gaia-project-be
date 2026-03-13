package com.gaiaproject.repository.player;

import com.gaiaproject.domain.entity.player.GamePlayerRoundBooster;
import com.gaiaproject.domain.enumtype.booster.RoundBoosterType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GamePlayerRoundBoosterRepository extends JpaRepository<GamePlayerRoundBooster, UUID> {

    Optional<GamePlayerRoundBooster> findByGameIdAndPlayerId(UUID gameId, UUID playerId);

    List<GamePlayerRoundBooster> findByGameId(UUID gameId);

    boolean existsByGameIdAndRoundBoosterType(UUID gameId, RoundBoosterType roundBoosterType);

    void deleteByGameId(UUID gameId);

    void deleteByGameIdAndPlayerId(UUID gameId, UUID playerId);
}