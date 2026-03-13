package com.gaiaproject.repository.tech;

import com.gaiaproject.domain.entity.player.GamePlayerTechTile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GamePlayerTechTileRepository extends JpaRepository<GamePlayerTechTile, UUID> {

    List<GamePlayerTechTile> findByGameId(UUID gameId);

    List<GamePlayerTechTile> findByGameIdAndPlayerId(UUID gameId, UUID playerId);

    List<GamePlayerTechTile> findByGameIdAndPlayerIdAndIsCovered(UUID gameId, UUID playerId, Boolean isCovered);

    Optional<GamePlayerTechTile> findByGameIdAndPlayerIdAndTechTileCode(UUID gameId, UUID playerId, String techTileCode);

    boolean existsByGameIdAndPlayerIdAndTechTileCode(UUID gameId, UUID playerId, String techTileCode);

    void deleteByGameId(UUID gameId);
}