package com.gaiaproject.repository.game;

import com.gaiaproject.domain.entity.game.GamePowerActionUsage;
import com.gaiaproject.domain.enumtype.action.PowerActionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GamePowerActionUsageRepository extends JpaRepository<GamePowerActionUsage, UUID> {

    List<GamePowerActionUsage> findByGameIdAndRoundNumber(UUID gameId, Integer roundNumber);

    Optional<GamePowerActionUsage> findByGameIdAndRoundNumberAndPowerActionType(
            UUID gameId, Integer roundNumber, PowerActionType powerActionType
    );

    boolean existsByGameIdAndRoundNumberAndPowerActionType(
            UUID gameId, Integer roundNumber, PowerActionType powerActionType
    );

    void deleteByGameId(UUID gameId);

    void deleteByGameIdAndRoundNumber(UUID gameId, Integer roundNumber);
}
