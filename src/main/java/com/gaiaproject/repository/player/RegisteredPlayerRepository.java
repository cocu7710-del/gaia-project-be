package com.gaiaproject.repository.player;

import com.gaiaproject.domain.entity.player.RegisteredPlayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

import java.util.Optional;
import java.util.UUID;

public interface RegisteredPlayerRepository extends JpaRepository<RegisteredPlayer, UUID> {
    Optional<RegisteredPlayer> findByNickname(String nickname);
    boolean existsByNicknameInAndIsRealPlayerFalse(List<String> nicknames);
}
