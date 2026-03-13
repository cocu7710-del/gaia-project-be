package com.gaiaproject.repository.artifact;

import com.gaiaproject.domain.entity.artifact.GameArtifactOffer;
import com.gaiaproject.domain.enumtype.artifact.ArtifactType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GameArtifactOfferRepository extends JpaRepository<GameArtifactOffer, UUID> {

    List<GameArtifactOffer> findByGameIdOrderByPosition(UUID gameId);

    List<GameArtifactOffer> findByGameIdAndIsAcquired(UUID gameId, Boolean isAcquired);

    Optional<GameArtifactOffer> findByGameIdAndArtifactType(UUID gameId, ArtifactType artifactType);

    void deleteByGameId(UUID gameId);
}
