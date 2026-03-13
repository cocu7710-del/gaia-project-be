package com.gaiaproject.repository.map;

import com.gaiaproject.domain.entity.map.GameSingleHexPlacement;
import com.gaiaproject.domain.entity.map.GameSingleHexPlacementId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GameSingleHexPlacementRepository extends JpaRepository<GameSingleHexPlacement, GameSingleHexPlacementId> {
    List<GameSingleHexPlacement> findByGameIdOrderByPositionNoAsc(UUID gameId);
}
