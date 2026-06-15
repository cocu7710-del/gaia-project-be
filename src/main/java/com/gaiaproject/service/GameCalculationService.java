package com.gaiaproject.service;

import com.gaiaproject.domain.entity.building.GameBuilding;
import com.gaiaproject.domain.entity.map.GameHex;
import com.gaiaproject.domain.enumtype.building.BuildingType;
import com.gaiaproject.domain.enumtype.player.PlanetType;
import com.gaiaproject.repository.building.GameBuildingRepository;
import com.gaiaproject.repository.map.GameHexRepository;
import com.gaiaproject.repository.player.GamePlayerArtifactRepository;
import com.gaiaproject.repository.tech.GamePlayerTechTileRepository;
import com.gaiaproject.repository.federation.GameFederationGroupRepository;
import com.gaiaproject.repository.federation.GameFederationBuildingRepository;
import com.gaiaproject.repository.federation.GameFederationTokenHexRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 게임 공통 계산 유틸리티
 * - 최종 미션 달성도
 * - 행성 종류 카운트
 * - 깊은 구역 섹터 수
 * - 인공물 가상 건물
 * - 기술 타일 보유 체크
 * - 건물 파워 값
 */
@Service
@RequiredArgsConstructor
public class GameCalculationService {

    private final GameBuildingRepository buildingRepository;
    private final GameHexRepository hexRepository;
    private final GamePlayerArtifactRepository playerArtifactRepository;
    private final GamePlayerTechTileRepository playerTechTileRepository;
    private final GameFederationGroupRepository federationGroupRepository;
    private final GameFederationBuildingRepository federationBuildingRepository;
    private final GameFederationTokenHexRepository federationTokenHexRepository;

    // ─────────────────────────────────────────
    // 기술 타일 보유 체크 (커버되지 않은 활성 타일)
    // ─────────────────────────────────────────
    public boolean hasActiveTechTile(UUID gameId, UUID playerId, String tileCode) {
        return playerTechTileRepository
                .findByGameIdAndPlayerIdAndIsCovered(gameId, playerId, false)
                .stream()
                .anyMatch(t -> tileCode.equals(t.getTechTileCode()));
    }

    // ─────────────────────────────────────────
    // 인공물 가상 건물/행성 체크
    // ─────────────────────────────────────────
    public boolean hasArtifact(UUID gameId, UUID playerId, String artifactType) {
        return playerArtifactRepository.existsByGameIdAndPlayerIdAndArtifactType(gameId, playerId, artifactType);
    }

    /** 인공물 포함 광산 수 */
    public int getMineCount(UUID gameId, UUID playerId, int stockMine) {
        int mines = 8 - stockMine;
        if (hasArtifact(gameId, playerId, "ARTIFACT_7")) mines++;
        if (hasArtifact(gameId, playerId, "ARTIFACT_8")) mines++;
        return mines;
    }

    /** 인공물 포함 건물 수 (가이아포머 제외) */
    public int getBuildingCount(List<GameBuilding> myBuildings, UUID gameId, UUID playerId) {
        int count = myBuildings.size();
        if (hasArtifact(gameId, playerId, "ARTIFACT_7")) count++;
        if (hasArtifact(gameId, playerId, "ARTIFACT_8")) count++;
        return count;
    }

    /** 인공물 포함 행성 종류 수 */
    public int getPlanetTypeCount(List<GameBuilding> myBuildings, Map<String, GameHex> hexByCoord, UUID gameId, UUID playerId) {
        Set<PlanetType> types = myBuildings.stream()
                .filter(b -> !b.isLantidsMine())
                .map(b -> hexByCoord.get(b.getHexQ() + "," + b.getHexR()))
                .filter(Objects::nonNull)
                .map(GameHex::getPlanetType)
                .filter(p -> p != PlanetType.EMPTY && p != PlanetType.TRANSDIM)
                .collect(Collectors.toSet());
        if (hasArtifact(gameId, playerId, "ARTIFACT_7")) types.add(PlanetType.ASTEROIDS);
        if (hasArtifact(gameId, playerId, "ARTIFACT_8")) types.add(PlanetType.LOST_PLANET);
        return types.size();
    }

    /** 인공물 포함 소행성 건물 수 */
    public int getAsteroidBuildingCount(List<GameBuilding> myBuildings, Map<String, GameHex> hexByCoord, UUID gameId, UUID playerId) {
        int count = (int) myBuildings.stream()
                .filter(b -> {
                    GameHex hex = hexByCoord.get(b.getHexQ() + "," + b.getHexR());
                    return hex != null && hex.getPlanetType() == PlanetType.ASTEROIDS;
                }).count();
        if (hasArtifact(gameId, playerId, "ARTIFACT_7")) count++;
        return count;
    }

    // ─────────────────────────────────────────
    // 깊은 구역 섹터 수 (건물이 있는 고유 섹터)
    // ─────────────────────────────────────────
    public int getDeepSectorCount(List<GameBuilding> myBuildings, Map<String, GameHex> hexByCoord) {
        Set<String> deepSectors = new HashSet<>();
        for (GameBuilding b : myBuildings) {
            GameHex hex = hexByCoord.get(b.getHexQ() + "," + b.getHexR());
            if (hex != null && hex.getSectorId() != null && hex.getSectorId().startsWith("DEEP_SECTOR")) {
                deepSectors.add(hex.getSectorId());
            }
        }
        return deepSectors.size();
    }

    // ─────────────────────────────────────────
    // 건물 파워 값
    // ─────────────────────────────────────────
    public int getBasePowerValue(BuildingType type) {
        return switch (type) {
            case MINE -> 1;
            case TRADING_STATION, RESEARCH_LAB -> 2;
            case PLANETARY_INSTITUTE, ACADEMY -> 3;
            case SPACE_STATION -> 1;
            default -> 0;
        };
    }

    public int getPowerValueWithModifiers(BuildingType type, UUID gameId, UUID playerId, boolean hasRing) {
        int base = getBasePowerValue(type);
        // BASIC_TILE_9: 큰 건물(PI, Academy) 파워값 +1
        if ((type == BuildingType.PLANETARY_INSTITUTE || type == BuildingType.ACADEMY)
                && hasActiveTechTile(gameId, playerId, "BASIC_TILE_9")) {
            base++;
        }
        // 모웨이드 링: +2
        if (hasRing) base += 2;
        return base;
    }

    // ─────────────────────────────────────────
    // 최종 미션 달성도 계산 (공통)
    // ─────────────────────────────────────────
    public int calcFinalProgress(UUID gameId, String tileCode, UUID playerId,
                                  List<GameBuilding> allBuildings, Map<String, GameHex> hexByCoord) {
        List<GameBuilding> myBuildings = allBuildings.stream()
                .filter(b -> b.getPlayerId().equals(playerId))
                .filter(b -> b.getBuildingType() != BuildingType.GAIAFORMER)
                .filter(b -> b.getBuildingType() != BuildingType.SPACE_STATION)
                .toList();

        return switch (tileCode) {
            case "FINAL_TILE_ASTEROID" -> getAsteroidBuildingCount(myBuildings, hexByCoord, gameId, playerId);

            case "FINAL_TILE_GAIA_PLANET" -> (int) myBuildings.stream()
                    .filter(b -> !b.isLantidsMine()) // 란티다 기생 광산은 행성 소유로 보지 않음
                    .filter(b -> {
                        GameHex hex = hexByCoord.get(b.getHexQ() + "," + b.getHexR());
                        return hex != null && hex.getPlanetType() == PlanetType.GAIA;
                    }).count();

            case "FINAL_TILE_MOST_BUILDINGS" -> getBuildingCount(myBuildings, gameId, playerId);

            case "FINAL_TILE_FEDERATION_BUILDINGS" -> {
                var groups = federationGroupRepository.findByGameIdAndPlayerId(gameId, playerId);
                if (groups.isEmpty()) yield 0;
                var groupIds = groups.stream().map(g -> g.getId()).toList();
                // 좌표 기준 중복 제거 + 우주정거장 제외
                var fedBuildings = federationBuildingRepository.findByFederationGroupIdIn(groupIds);
                var buildingMap = allBuildings.stream()
                        .collect(java.util.stream.Collectors.toMap(
                                b -> b.getHexQ() + "," + b.getHexR(), b -> b, (a, c) -> a));
                yield (int) fedBuildings.stream()
                        .map(b -> b.getHexQ() + "," + b.getHexR())
                        .distinct()
                        .filter(key -> {
                            var building = buildingMap.get(key);
                            return building != null && building.getBuildingType() != BuildingType.SPACE_STATION;
                        })
                        .count();
            }

            case "FINAL_TILE_DEEP_SECTORS" -> getDeepSectorCount(myBuildings, hexByCoord);

            case "FINAL_TILE_PLANET_TYPES" -> getPlanetTypeCount(myBuildings, hexByCoord, gameId, playerId);

            case "FINAL_TILE_FEDERATION_POWER" -> {
                var groups = federationGroupRepository.findByGameIdAndPlayerId(gameId, playerId);
                int tokenCount = 0;
                for (var g : groups) tokenCount += federationTokenHexRepository.findByFederationGroupId(g.getId()).size();
                yield tokenCount;
            }

            case "FINAL_TILE_PI_ACADEMY_DISTANCE" -> {
                var pis = myBuildings.stream()
                        .filter(b -> b.getBuildingType() == BuildingType.PLANETARY_INSTITUTE).toList();
                var academies = myBuildings.stream()
                        .filter(b -> b.getBuildingType() == BuildingType.ACADEMY).toList();
                if (pis.isEmpty() || academies.isEmpty()) yield 0;
                int maxDist = 0;
                for (var pi : pis) {
                    for (var ac : academies) {
                        int d = hexDistance(pi.getHexQ(), pi.getHexR(), ac.getHexQ(), ac.getHexR());
                        if (d > maxDist) maxDist = d;
                    }
                }
                yield maxDist;
            }

            case "FINAL_TILE_SECTORS_WITH_BUILDINGS" -> {
                Set<String> sectors = myBuildings.stream()
                        .map(b -> hexByCoord.get(b.getHexQ() + "," + b.getHexR()))
                        .filter(Objects::nonNull)
                        .map(GameHex::getSectorId)
                        .filter(s -> s != null && s.startsWith("SECTOR_"))
                        .collect(Collectors.toSet());
                yield sectors.size();
            }

            default -> 0;
        };
    }

    private int hexDistance(int q1, int r1, int q2, int r2) {
        int dq = q2 - q1, dr = r2 - r1;
        return (Math.abs(dq) + Math.abs(dr) + Math.abs(dq + dr)) / 2;
    }
}
