package com.gaiaproject.repository.map;

import com.gaiaproject.domain.entity.map.GameSectorPlacement;
import com.gaiaproject.domain.entity.map.GameSectorPlacementId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface GameSectorPlacementRepository extends JpaRepository<GameSectorPlacement, GameSectorPlacementId> {
    List<GameSectorPlacement> findByGameIdOrderByPositionNoAsc(UUID gameId);
}
