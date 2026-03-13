package com.gaiaproject.repository.player;

import com.gaiaproject.domain.entity.player.Player;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PlayerRepository extends JpaRepository<Player, UUID> {
}
