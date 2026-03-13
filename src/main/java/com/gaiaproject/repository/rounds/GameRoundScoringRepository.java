package com.gaiaproject.repository.rounds;

import com.gaiaproject.domain.entity.rounds.GameRoundScoring;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GameRoundScoringRepository extends JpaRepository<GameRoundScoring, UUID> {

    List<GameRoundScoring> findByGameIdOrderByRoundNumber(UUID gameId);

    java.util.Optional<GameRoundScoring> findByGameIdAndRoundNumber(UUID gameId, Integer roundNumber);

    void deleteByGameId(UUID gameId);
}