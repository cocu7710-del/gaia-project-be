package com.gaiaproject.repository.tech;

import com.gaiaproject.domain.entity.tech.GameTechOffer;
import com.gaiaproject.domain.enumtype.tech.TechTileCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GameTechOfferRepository extends JpaRepository<GameTechOffer, UUID> {

    List<GameTechOffer> findByGameIdOrderByPosition(UUID gameId);

    Optional<GameTechOffer> findByGameIdAndTechTileCode(UUID gameId, TechTileCode techTileCode);

    void deleteByGameId(UUID gameId);
}