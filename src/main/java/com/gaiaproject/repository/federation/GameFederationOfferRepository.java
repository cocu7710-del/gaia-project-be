package com.gaiaproject.repository.federation;

import com.gaiaproject.domain.entity.federation.GameFederationOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GameFederationOfferRepository extends JpaRepository<GameFederationOffer, UUID> {

    List<GameFederationOffer> findByGameId(UUID gameId);

    void deleteByGameId(UUID gameId);
}