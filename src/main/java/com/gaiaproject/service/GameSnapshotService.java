package com.gaiaproject.service;

import com.gaiaproject.domain.entity.game.Game;
import com.gaiaproject.domain.enumtype.federation.FederationTileType;
import com.gaiaproject.domain.enumtype.rounds.FinalScoringTileType;
import com.gaiaproject.domain.enumtype.rounds.RoundScoringTileType;
import com.gaiaproject.domain.enumtype.tech.AdvancedTechTileCode;
import com.gaiaproject.domain.enumtype.tech.TechTileCode;
import com.gaiaproject.dto.TechAbility;
import com.gaiaproject.dto.response.GameSnapshot;
import com.gaiaproject.repository.artifact.GameArtifactOfferRepository;
import com.gaiaproject.repository.booster.GameBoosterOfferRepository;
import com.gaiaproject.repository.building.GameBuildingRepository;
import com.gaiaproject.repository.federation.GameFederationBuildingRepository;
import com.gaiaproject.repository.federation.GameFederationGroupRepository;
import com.gaiaproject.repository.federation.GameFederationOfferRepository;
import com.gaiaproject.repository.federation.GameFederationTokenHexRepository;
import com.gaiaproject.repository.game.GamePlayerPassRepository;
import com.gaiaproject.repository.game.GameRepository;
import com.gaiaproject.repository.game.GameSeatRepository;
import com.gaiaproject.repository.map.GameHexRepository;
import com.gaiaproject.repository.player.GamePlayerArtifactRepository;
import com.gaiaproject.repository.player.GamePlayerFederationTokenRepository;
import com.gaiaproject.repository.player.GamePlayerFleetProbeRepository;
import com.gaiaproject.repository.player.GamePlayerStateRepository;
import com.gaiaproject.repository.tech.GamePlayerTechTileRepository;
import com.gaiaproject.repository.rounds.GameFinalScoringRepository;
import com.gaiaproject.repository.rounds.GameRoundScoringRepository;
import com.gaiaproject.repository.tech.GameAdvTechOfferRepository;
import com.gaiaproject.repository.tech.GameTechOfferRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 게임 전체 스냅샷 빌더 (C안 long-term).
 *
 * WS broadcastStateUpdated에 전달되어 FE가 fetch 없이 상태 동기화.
 * hot + cold 데이터 모두 포함.
 */
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class GameSnapshotService {

    private final GameRepository gameRepository;
    private final GameSeatRepository gameSeatRepository;
    private final GamePlayerStateRepository playerStateRepository;
    private final GameHexRepository hexRepository;
    private final GameBuildingRepository buildingRepository;
    private final GameFederationGroupRepository federationGroupRepository;
    private final GameFederationBuildingRepository federationBuildingRepository;
    private final GameFederationTokenHexRepository federationTokenHexRepository;
    private final GameFederationOfferRepository federationOfferRepository;
    private final GamePlayerPassRepository playerPassRepository;
    private final GameTechOfferRepository techOfferRepository;
    private final GameAdvTechOfferRepository advTechOfferRepository;
    private final GamePlayerTechTileRepository playerTechTileRepository;
    private final GamePlayerArtifactRepository playerArtifactRepository;
    private final GamePlayerFederationTokenRepository playerFederationTokenRepository;
    private final GameArtifactOfferRepository artifactOfferRepository;
    private final GameBoosterOfferRepository boosterOfferRepository;
    private final GamePlayerFleetProbeRepository fleetProbeRepository;
    private final GameRoundScoringRepository roundScoringRepository;
    private final GameFinalScoringRepository finalScoringRepository;
    private final PowerActionService powerActionService;

    public GameSnapshot buildSnapshot(UUID gameId) {
        Game game = gameRepository.findById(gameId).orElse(null);
        if (game == null) {
            log.warn("[SNAPSHOT] 게임 없음: {}", gameId);
            return null;
        }

        // ===== HOT =====

        // 1. Seats
        var seats = gameSeatRepository.findByGameIdOrderBySeatNo(gameId).stream()
                .map(s -> new GameSnapshot.SeatDto(
                        s.getSeatNo(),
                        s.getTurnOrder(),
                        s.getFactionType() != null ? s.getFactionType().name() : null,
                        s.getFactionType() != null ? s.getFactionType().getDisplayNameKo() : null,
                        s.getFactionType() != null ? s.getFactionType().getHomePlanet().name() : null,
                        s.getPlayerId(),
                        s.getNickname()
                ))
                .toList();

        // 2. Player states (with artifacts + federation tokens)
        var allPlayerStates = playerStateRepository.findByGameId(gameId);
        List<GameSnapshot.PlayerStateDto> playerStates = new ArrayList<>();
        for (var ps : allPlayerStates) {
            var artifacts = playerArtifactRepository.findByGameIdAndPlayerId(gameId, ps.getPlayerId())
                    .stream().map(a -> a.getArtifactType()).toList();
            var fedTokens = playerFederationTokenRepository.findByGameIdAndPlayerId(gameId, ps.getPlayerId())
                    .stream()
                    .map(t -> new GameSnapshot.PlayerFederationTokenDto(
                            t.getFederationTileType().name(),
                            t.isUsed()
                    ))
                    .toList();
            playerStates.add(new GameSnapshot.PlayerStateDto(
                    ps.getPlayerId(),
                    ps.getSeatNo(),
                    ps.getFactionType() != null ? ps.getFactionType().name() : null,
                    ps.getCredit(), ps.getOre(), ps.getKnowledge(), ps.getQic(),
                    ps.getPowerBowl1(), ps.getPowerBowl2(), ps.getPowerBowl3(),
                    ps.getBrainstoneBowl(),
                    ps.getGaiaPower(),
                    ps.getVictoryPoints(),
                    ps.getTechTerraforming(), ps.getTechNavigation(), ps.getTechAi(),
                    ps.getTechGaia(), ps.getTechEconomy(), ps.getTechScience(),
                    ps.getStockMine(), ps.getStockTradingStation(), ps.getStockResearchLab(),
                    ps.getStockPlanetaryInstitute(), ps.getStockAcademy(), ps.getStockGaiaformer(),
                    ps.isBoosterActionUsed(), ps.isFactionAbilityUsed(), ps.isQicAcademyActionUsed(),
                    ps.isGleensHasQicAcademy(),
                    ps.isGleensHasQicAcademy(), // hasQicAcademy 폴백
                    ps.getBaltaksConvertedGaiaformers(), ps.getPermanentlyRemovedGaiaformers(),
                    ps.getFederationCount(),
                    ps.getBidPenalty(),
                    ps.getTinkeroidsUsedActions(), ps.getTinkeroidsCurrentAction(),
                    ps.getUsedTimeSeconds(),
                    ps.getTurnStartedAt() != null
                            ? ps.getTurnStartedAt().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                            : null,
                    artifacts,
                    fedTokens
            ));
        }

        // 3. Hexes
        var hexes = hexRepository.findByGameId(gameId).stream()
                .map(h -> new GameSnapshot.HexDto(
                        h.getHexQ(), h.getHexR(),
                        h.getPlanetType() != null ? h.getPlanetType().name() : null,
                        h.getSectorId(),
                        h.getPositionNo()
                ))
                .toList();

        // 4. Buildings
        var buildings = buildingRepository.findByGameId(gameId).stream()
                .map(b -> new GameSnapshot.BuildingDto(
                        b.getId(),
                        b.getPlayerId(),
                        b.getHexQ(), b.getHexR(),
                        b.getBuildingType() != null ? b.getBuildingType().name() : null,
                        b.isLantidsMine(),
                        b.isHasRing(),
                        b.getAcademyType() != null ? b.getAcademyType().name() : null
                ))
                .toList();

        // 5. Federation groups
        List<GameSnapshot.FederationGroupDto> fedGroups = new ArrayList<>();
        for (var g : federationGroupRepository.findByGameId(gameId)) {
            var bldHexes = federationBuildingRepository.findByFederationGroupId(g.getId()).stream()
                    .map(fb -> new int[]{fb.getHexQ(), fb.getHexR()})
                    .toList();
            var tokHexes = federationTokenHexRepository.findByFederationGroupId(g.getId()).stream()
                    .map(ft -> new int[]{ft.getHexQ(), ft.getHexR()})
                    .toList();
            fedGroups.add(new GameSnapshot.FederationGroupDto(
                    g.getId(), g.getPlayerId(), g.getFederationTileCode(),
                    bldHexes, tokHexes, g.isUsed()
            ));
        }

        // 6. Passed seat numbers
        int currentRound = game.getCurrentRound() != null ? game.getCurrentRound() : 1;
        List<Integer> passedSeatNos = playerPassRepository
                .findByGameIdAndRoundNumber(gameId, currentRound)
                .stream()
                .map(p -> {
                    var seat = gameSeatRepository.findByGameIdAndPlayerId(gameId, p.getPlayerId()).orElse(null);
                    return seat != null ? seat.getSeatNo() : null;
                })
                .filter(java.util.Objects::nonNull)
                .toList();

        // 6b. 특수 페이즈 복원 정보 (ITARS / TINKEROIDS)
        UUID pendingSpecialPlayerId = null;
        Map<String, Object> pendingSpecialData = null;
        String gamePhaseStr = game.getGamePhase();
        if ("ITARS_GAIA_PHASE".equals(gamePhaseStr)) {
            var itarsPlayer = allPlayerStates.stream()
                    .filter(p -> p.getFactionType() == com.gaiaproject.domain.enumtype.player.FactionType.ITARS)
                    .findFirst().orElse(null);
            if (itarsPlayer != null) {
                pendingSpecialPlayerId = itarsPlayer.getPlayerId();
                pendingSpecialData = Map.of("availableChoices", itarsPlayer.getGaiaPower() / 4);
            }
        } else if ("TINKEROIDS_ACTION_PHASE".equals(gamePhaseStr)) {
            var tinkPlayer = allPlayerStates.stream()
                    .filter(p -> p.getFactionType() == com.gaiaproject.domain.enumtype.player.FactionType.TINKEROIDS)
                    .findFirst().orElse(null);
            if (tinkPlayer != null) {
                pendingSpecialPlayerId = tinkPlayer.getPlayerId();
                List<String> pool = currentRound <= 3
                        ? List.of("TINK_TERRAFORM_1", "TINK_POWER_4", "TINK_QIC_1")
                        : List.of("TINK_TERRAFORM_3", "TINK_KNOWLEDGE_3", "TINK_QIC_2");
                List<String> available = pool.stream().filter(a -> !tinkPlayer.isTinkeroidsActionUsed(a)).toList();
                pendingSpecialData = Map.of("availableActions", available, "currentRound", currentRound);
            }
        }

        // ===== COLD =====

        // 7. Basic tile offers (with ownership)
        var allPlayerTiles = playerTechTileRepository.findByGameId(gameId);
        Map<String, List<UUID>> tileOwners = new HashMap<>();
        Map<String, List<UUID>> tileActionUsed = new HashMap<>();
        Map<String, Map<String, String>> tileCoveredByMap = new HashMap<>();
        for (var pt : allPlayerTiles) {
            String code = pt.getTechTileCode();
            // 덮인 타일이라도 소유권은 유지 (같은 기본 타일 중복 획득 불가 룰)
            tileOwners.computeIfAbsent(code, k -> new ArrayList<>()).add(pt.getPlayerId());
            if (Boolean.TRUE.equals(pt.getIsCovered())) {
                tileCoveredByMap.computeIfAbsent(code, k -> new HashMap<>())
                        .put(pt.getPlayerId().toString(), pt.getCoveredBy());
            } else if (Boolean.TRUE.equals(pt.getActionUsed())) {
                tileActionUsed.computeIfAbsent(code, k -> new ArrayList<>()).add(pt.getPlayerId());
            }
        }

        var basicTileOffers = techOfferRepository.findByGameIdOrderByPosition(gameId).stream()
                .map(o -> {
                    TechAbility ability = o.getTechTileCode().getAbility();
                    String codeStr = o.getTechTileCode().name();
                    return new GameSnapshot.TechOfferDto(
                            o.getPosition(),
                            o.getTechTrack(),
                            codeStr,
                            ability != null ? ability.getType().name() : null,
                            ability != null ? ability.getDescription() : null,
                            tileOwners.getOrDefault(codeStr, List.of()),
                            tileActionUsed.getOrDefault(codeStr, List.of()),
                            tileCoveredByMap.getOrDefault(codeStr, Map.of())
                    );
                })
                .toList();

        // 8. Advanced tile offers
        var advTileOffers = advTechOfferRepository.findByGameIdOrderByPosition(gameId).stream()
                .map(o -> {
                    TechAbility ability = o.getAdvTechTileCode().getAbility();
                    // 고급 타일의 action used 여부: 해당 플레이어가 소유한 adv tile의 action_used
                    boolean actionUsed = allPlayerTiles.stream()
                            .filter(pt -> pt.getTechTileCode().equals(o.getAdvTechTileCode().name()))
                            .anyMatch(pt -> Boolean.TRUE.equals(pt.getActionUsed()));
                    return new GameSnapshot.AdvTechOfferDto(
                            o.getPosition(),
                            o.getTechTrack(),
                            o.getAdvTechTileCode().name(),
                            ability != null ? ability.getDescription() : null,
                            o.getTakenByPlayerId() != null,
                            o.getTakenByPlayerId(),
                            actionUsed,
                            ability != null && ability.getType() != null ? ability.getType().name() : null
                    );
                })
                .toList();

        // 9. Federation tile supply
        //   - position == null → general supply (9장)
        //   - position == 0    → 테라포밍 트랙 꼭대기 타일
        //   - position 1~4     → 잊힌 함대 연방 타일
        var allFedOffers = federationOfferRepository.findByGameId(gameId);
        List<GameSnapshot.FederationTileOfferDto> generalFed = new ArrayList<>();
        List<GameSnapshot.FederationTileOfferDto> forgottenFleet = new ArrayList<>();
        GameSnapshot.FederationTileOfferDto terraTrackTile = null;
        for (var offer : allFedOffers) {
            var dto = new GameSnapshot.FederationTileOfferDto(
                    offer.getFederationTileType().name(),
                    offer.getQuantity(),
                    offer.getPosition(),
                    null
            );
            Integer pos = offer.getPosition();
            if (pos == null) {
                generalFed.add(dto);
            } else if (pos == 0) {
                terraTrackTile = dto;
            } else if (pos >= 1 && pos <= 4) {
                forgottenFleet.add(dto);
            } else {
                generalFed.add(dto);
            }
        }

        // 10. Artifact offers
        var artifactOffers = artifactOfferRepository.findByGameIdOrderByPosition(gameId).stream()
                .map(a -> new GameSnapshot.ArtifactOfferDto(
                        a.getArtifactType().name(),
                        a.getPosition(),
                        Boolean.TRUE.equals(a.getIsAcquired()),
                        a.getAcquiredBy(),
                        null
                ))
                .toList();

        // 11. Booster offers
        var boosterOffers = boosterOfferRepository.findByGameIdOrderByPositionAsc(gameId).stream()
                .map(o -> new GameSnapshot.BoosterOfferDto(
                        o.getBoosterCode(),
                        o.getPickedBySeatNo(),
                        null
                ))
                .toList();

        // 12. Fleet probes (grouped by fleet name)
        Map<String, List<UUID>> probesByFleet = new HashMap<>();
        for (var probe : fleetProbeRepository.findByGameIdOrderByPlacedAtAsc(gameId)) {
            probesByFleet.computeIfAbsent(probe.getFleetName(), k -> new ArrayList<>()).add(probe.getPlayerId());
        }
        List<GameSnapshot.FleetProbeDto> fleetProbeList = new ArrayList<>();
        for (var entry : probesByFleet.entrySet()) {
            fleetProbeList.add(new GameSnapshot.FleetProbeDto(entry.getKey(), entry.getValue()));
        }

        // 13. Round scoring tiles
        var roundScoringTiles = roundScoringRepository.findByGameIdOrderByRoundNumber(gameId).stream()
                .map(r -> new GameSnapshot.RoundScoringTileDto(
                        r.getRoundNumber(),
                        r.getScoringTileCode().name(),
                        r.getScoringTileCode().getDescription()
                ))
                .toList();

        // 14-0. Used power/fleet action codes (현재 라운드)
        List<String> usedPowerActionCodes = powerActionService.getUsedPowerActionCodes(gameId);

        // 14. Final scoring tiles
        var finalScoringTiles = finalScoringRepository.findByGameIdOrderByPosition(gameId).stream()
                .map(f -> new GameSnapshot.FinalScoringTileDto(
                        f.getPosition(),
                        f.getScoringTileCode().name(),
                        f.getScoringTileCode().getDescription(),
                        new HashMap<>()  // progress는 별도 계산 필요 — 당장은 빈 맵
                ))
                .toList();

        return new GameSnapshot(
                gameId,
                game.getGamePhase(),
                game.getStatus(),
                game.getCurrentRound(),
                game.getCurrentTurnSeatNo(),
                game.getCurrentSetupSeatNo(),
                game.getEconomyTrackOption() != null ? game.getEconomyTrackOption().name() : null,
                game.getCommonAdvTileCondition() != null ? game.getCommonAdvTileCondition().name() : null,
                game.getTinkeroidsExtraRingPlanet(),
                game.getMoweidsExtraRingPlanet(),
                pendingSpecialPlayerId,
                pendingSpecialData,
                passedSeatNos,
                seats,
                playerStates,
                hexes,
                buildings,
                fedGroups,
                basicTileOffers,
                advTileOffers,
                generalFed,
                forgottenFleet,
                terraTrackTile,
                artifactOffers,
                boosterOffers,
                fleetProbeList,
                roundScoringTiles,
                finalScoringTiles,
                usedPowerActionCodes
        );
    }
}
