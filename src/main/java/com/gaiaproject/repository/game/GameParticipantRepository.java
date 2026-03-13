package com.gaiaproject.repository.game;

import com.gaiaproject.domain.entity.game.GameParticipant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface GameParticipantRepository extends JpaRepository<GameParticipant, UUID> {

    Optional<GameParticipant> findByGameIdAndPlayerId(UUID gameId, UUID playerId);

    List<GameParticipant> findByGameIdOrderByEnteredAtAsc(UUID gameId);

    long countByGameId(UUID gameId);

    @Query("SELECT COUNT(gp) > 0 FROM GameParticipant gp JOIN Player p ON gp.playerId = p.id " +
            "WHERE gp.gameId = :gameId AND p.nickname = :nickname")
    boolean existsByGameIdAndNickname(@Param("gameId") UUID gameId, @Param("nickname") String nickname);

    @Query("SELECT gp FROM GameParticipant gp JOIN Player p ON gp.playerId = p.id " +
            "WHERE gp.gameId = :gameId AND p.nickname = :nickname")
    Optional<GameParticipant> findByGameIdAndNickname(@Param("gameId") UUID gameId, @Param("nickname") String nickname);
}
