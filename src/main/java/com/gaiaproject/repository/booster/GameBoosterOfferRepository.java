package com.gaiaproject.repository.booster;

import com.gaiaproject.domain.entity.booster.GameBoosterOffer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameBoosterOfferRepository extends JpaRepository<GameBoosterOffer, UUID> {

    long countByGameId(UUID gameId);

    boolean existsByGameId(UUID gameId);

    List<GameBoosterOffer> findByGameIdOrderByPositionAsc(UUID gameId);

    boolean existsByGameIdAndBoosterCode(UUID gameId, String boosterCode);

    Optional<GameBoosterOffer> findByGameIdAndBoosterCode(UUID gameId, String boosterCode);

    long countByGameIdAndPickedBySeatNoIsNotNull(UUID gameId);
}