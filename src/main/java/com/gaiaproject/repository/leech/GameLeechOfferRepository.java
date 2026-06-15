package com.gaiaproject.repository.leech;

import com.gaiaproject.domain.entity.leech.GameLeechOffer;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GameLeechOfferRepository extends JpaRepository<GameLeechOffer, UUID> {
    List<GameLeechOffer> findByGameIdAndBatchKeyOrderBySequenceNo(UUID gameId, String batchKey);
    Optional<GameLeechOffer> findFirstByGameIdAndBatchKeyAndStatusOrderBySequenceNo(UUID gameId, String batchKey, String status);
    List<GameLeechOffer> findByGameIdAndStatus(UUID gameId, String status);

    /**
     * 배치 내 모든 offer 를 PESSIMISTIC_WRITE 로 잠근다.
     * decidePowerLeech 동시 호출 시 race condition 방지용 — 동일 batch 의 결정은 직렬화됨.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM GameLeechOffer o WHERE o.gameId = :gameId AND o.batchKey = :batchKey ORDER BY o.sequenceNo")
    List<GameLeechOffer> lockBatchForUpdate(@Param("gameId") UUID gameId, @Param("batchKey") String batchKey);
}
