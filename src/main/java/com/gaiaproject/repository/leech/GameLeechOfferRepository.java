package com.gaiaproject.repository.leech;

import com.gaiaproject.domain.entity.leech.GameLeechOffer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GameLeechOfferRepository extends JpaRepository<GameLeechOffer, UUID> {
    List<GameLeechOffer> findByGameIdAndBatchKeyOrderBySequenceNo(UUID gameId, String batchKey);
    Optional<GameLeechOffer> findFirstByGameIdAndBatchKeyAndStatusOrderBySequenceNo(UUID gameId, String batchKey, String status);
    List<GameLeechOffer> findByGameIdAndStatus(UUID gameId, String status);
}
