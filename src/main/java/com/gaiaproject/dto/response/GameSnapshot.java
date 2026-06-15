package com.gaiaproject.dto.response;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 게임 전체 상태 스냅샷 (C안 long-term)
 *
 * WS 이벤트 payload에 실려서 broadcast. FE는 받자마자 local state 일괄 교체.
 * 목적: API fetch 없이 WS 이벤트만으로 완전한 상태 동기화.
 *
 * 포함 (모든 hot + cold state):
 * - 게임 메타 (phase, round, turn, passed)
 * - 좌석 / 플레이어 상태 (인공물 / 연방토큰 포함)
 * - 맵 (hexes, buildings)
 * - 연방 그룹
 * - 기술 타일 오퍼 (basic + advanced)
 * - 연방 타일 공급 (general / forgotten fleet / terra track / artifacts)
 * - 라운드 부스터 공급
 * - 함대 점유
 * - 라운드 / 최종 점수 타일
 */
public record GameSnapshot(
        UUID gameId,
        String gamePhase,
        String status,
        Integer currentRound,
        Integer currentTurnSeatNo,
        Integer nextSetupSeatNo,
        String economyTrackOption,
        String commonAdvTileCondition,
        String tinkeroidsExtraRingPlanet,
        String moweidsExtraRingPlanet,
        UUID pendingSpecialPlayerId,
        Map<String, Object> pendingSpecialData,
        List<Integer> passedSeatNos,

        List<SeatDto> seats,
        List<PlayerStateDto> playerStates,
        List<HexDto> hexes,
        List<BuildingDto> buildings,
        List<FederationGroupDto> federationGroups,

        // Cold data
        List<TechOfferDto> basicTileOffers,
        List<AdvTechOfferDto> advTileOffers,
        List<FederationTileOfferDto> generalFedSupply,
        List<FederationTileOfferDto> forgottenFleetTokens,
        FederationTileOfferDto terraformingTrackTile,
        List<ArtifactOfferDto> artifactOffers,
        List<BoosterOfferDto> roundBoosterOffers,
        List<FleetProbeDto> fleetProbes,
        List<RoundScoringTileDto> roundScoringTiles,
        List<FinalScoringTileDto> finalScoringTiles,
        List<String> usedPowerActionCodes
) {
    public record SeatDto(
            int seatNo,
            int turnOrder,
            String factionCode,
            String raceNameKo,
            String homePlanetType,
            UUID playerId,
            String nickname
    ) {}

    public record PlayerStateDto(
            UUID playerId,
            int seatNo,
            String factionCode,
            int credit, int ore, int knowledge, int qic,
            int powerBowl1, int powerBowl2, int powerBowl3,
            Integer brainstoneBowl,
            int gaiaPower,
            int victoryPoints,
            int techTerraforming, int techNavigation, int techAi, int techGaia, int techEconomy, int techScience,
            int stockMine, int stockTradingStation, int stockResearchLab, int stockPlanetaryInstitute,
            int stockAcademy, int stockGaiaformer,
            boolean boosterActionUsed, boolean factionAbilityUsed, boolean qicAcademyActionUsed,
            boolean gleensHasQicAcademy, boolean hasQicAcademy,
            int baltaksConvertedGaiaformers, int permanentlyRemovedGaiaformers,
            int federationCount,
            int bidPenalty,
            String tinkeroidsUsedActions, String tinkeroidsCurrentAction,
            int usedTimeSeconds, Long turnStartedAt,
            List<String> artifacts,
            List<PlayerFederationTokenDto> federationTokens
    ) {}

    public record PlayerFederationTokenDto(
            String tileType,
            boolean used
    ) {}

    public record HexDto(
            int hexQ, int hexR,
            String planetType,
            String sectorId,
            Integer positionNo
    ) {}

    public record BuildingDto(
            UUID id,
            UUID playerId,
            int hexQ, int hexR,
            String buildingType,
            boolean isLantidsMine,
            boolean hasRing,
            String academyType
    ) {}

    public record FederationGroupDto(
            UUID id,
            UUID playerId,
            String tileCode,
            List<int[]> buildingHexes,
            List<int[]> tokenHexes,
            boolean used
    ) {}

    public record TechOfferDto(
            Integer position,
            String trackCode,
            String tileCode,
            String abilityType,
            String description,
            List<UUID> ownerPlayerIds,
            List<UUID> actionUsedByPlayerIds,
            Map<String, String> coveredByMap
    ) {}

    public record AdvTechOfferDto(
            Integer position,
            String trackCode,
            String tileCode,
            String description,
            boolean isTaken,
            UUID takenByPlayerId,
            boolean isActionUsed,
            String abilityType  // ACTION / IMMEDIATE / PASSIVE / INCOME 등
    ) {}

    public record FederationTileOfferDto(
            String tileCode,
            int quantity,
            Integer position,
            String description
    ) {}

    public record ArtifactOfferDto(
            String artifactCode,
            int position,
            boolean isTaken,
            UUID acquiredByPlayerId,
            String description
    ) {}

    public record BoosterOfferDto(
            String boosterCode,
            Integer pickedBySeatNo,
            String description
    ) {}

    public record FleetProbeDto(
            String fleetName,
            List<UUID> playerIds
    ) {}

    public record RoundScoringTileDto(
            int roundNumber,
            String tileCode,
            String description
    ) {}

    public record FinalScoringTileDto(
            int position,
            String tileCode,
            String description,
            Map<String, Integer> playerProgress
    ) {}
}
