package com.gaiaproject.repository.rounds;

import com.gaiaproject.domain.entity.rounds.GameFinalScoring;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GameFinalScoringRepository extends JpaRepository<GameFinalScoring, UUID> {

    List<GameFinalScoring> findByGameIdOrderByPosition(UUID gameId);

    void deleteByGameId(UUID gameId);
}