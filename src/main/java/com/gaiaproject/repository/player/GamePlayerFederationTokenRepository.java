package com.gaiaproject.repository.player;

import com.gaiaproject.domain.entity.player.GamePlayerFederationToken;
import com.gaiaproject.domain.enumtype.federation.FederationTileType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GamePlayerFederationTokenRepository extends JpaRepository<GamePlayerFederationToken, UUID> {

    List<GamePlayerFederationToken> findByGameIdAndPlayerId(UUID gameId, UUID playerId);

    int countByGameIdAndPlayerId(UUID gameId, UUID playerId);

    boolean existsByGameIdAndPlayerIdAndFederationTileType(UUID gameId, UUID playerId, FederationTileType federationTileType);

    void deleteByGameId(UUID gameId);
}