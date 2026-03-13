package com.gaiaproject.repository.map;

import com.gaiaproject.domain.entity.map.GameHex;
import com.gaiaproject.domain.enumtype.player.PlanetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GameHexRepository extends JpaRepository<GameHex, GameHex.GameHexId> {

    /**
     * 특정 좌표의 헥스 조회
     */
    Optional<GameHex> findByGameIdAndHexQAndHexR(UUID gameId, int hexQ, int hexR);

    /**
     * 게임의 모든 헥스 조회
     */
    List<GameHex> findByGameId(UUID gameId);

    /**
     * 특정 행성 타입의 헥스 조회
     */
    List<GameHex> findByGameIdAndPlanetType(UUID gameId, PlanetType planetType);

    /**
     * 특정 좌표에 헥스가 있는지 확인
     */
    boolean existsByGameIdAndHexQAndHexR(UUID gameId, int hexQ, int hexR);

    /**
     * 특정 섹터의 헥스 조회
     */
    List<GameHex> findByGameIdAndSectorId(UUID gameId, String sectorId);
}
