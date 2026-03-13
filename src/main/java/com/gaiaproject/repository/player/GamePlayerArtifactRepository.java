package com.gaiaproject.repository.player;

import com.gaiaproject.domain.entity.player.GamePlayerArtifact;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GamePlayerArtifactRepository extends JpaRepository<GamePlayerArtifact, UUID> {

    List<GamePlayerArtifact> findByGameIdAndPlayerId(UUID gameId, UUID playerId);

    int countByGameIdAndPlayerId(UUID gameId, UUID playerId);

    boolean existsByGameIdAndPlayerIdAndArtifactType(UUID gameId, UUID playerId, String artifactType);

    void deleteByGameId(UUID gameId);
}