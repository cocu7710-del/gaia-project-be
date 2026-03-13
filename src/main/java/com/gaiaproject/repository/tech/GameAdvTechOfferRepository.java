package com.gaiaproject.repository.tech;

import com.gaiaproject.domain.entity.tech.GameAdvTechOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GameAdvTechOfferRepository  extends JpaRepository<GameAdvTechOffer, UUID> {

    List<GameAdvTechOffer> findByGameIdOrderByPosition(UUID gameId);

    void deleteByGameId(UUID gameId);
}