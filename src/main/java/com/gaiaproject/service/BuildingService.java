package com.gaiaproject.service;

import com.gaiaproject.domain.entity.building.GameBuilding;
import com.gaiaproject.domain.entity.game.Game;
import com.gaiaproject.domain.entity.game.GameSeat;
import com.gaiaproject.domain.entity.map.GameHex;
import com.gaiaproject.domain.entity.player.GamePlayerState;
import com.gaiaproject.domain.enumtype.action.ActionType;
import com.gaiaproject.domain.enumtype.building.BuildingType;
import com.gaiaproject.domain.enumtype.player.FactionType;
import com.gaiaproject.domain.enumtype.action.VpCategory;
import com.gaiaproject.domain.enumtype.player.PlanetType;
import com.gaiaproject.domain.enumtype.rounds.RoundScoringEvent;
import com.gaiaproject.dto.request.PlaceInitialMineRequest;
import com.gaiaproject.dto.request.PlaceMinePlayRequest;
import com.gaiaproject.dto.request.UpgradeBuildingRequest;
import com.gaiaproject.dto.response.PlaceInitialMineResponse;
import com.gaiaproject.dto.response.PlaceMinePlayResponse;
import com.gaiaproject.dto.response.UpgradeBuildingResponse;
import com.gaiaproject.repository.building.GameBuildingRepository;
import com.gaiaproject.repository.game.GameRepository;
import com.gaiaproject.repository.game.GameSeatRepository;
import com.gaiaproject.repository.map.GameHexRepository;
import com.gaiaproject.repository.player.GamePlayerStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BuildingService {

    private final GameRepository gameRepository;
    private final GameSeatRepository gameSeatRepository;
    private final GamePlayerStateRepository gamePlayerStateRepository;
    private final GameBuildingRepository gameBuildingRepository;
    private final GameHexRepository gameHexRepository;
    private final IncomeService incomeService;
    private final GameWebSocketService webSocketService;
    private final ActionService actionService;
    private final TechTileService techTileService;
    private final RoundScoringService roundScoringService;
    private final PowerLeechService powerLeechService;
    private final com.gaiaproject.repository.player.GamePlayerFederationTokenRepository federationTokenRepository;
    private final FederationFormService federationFormService;
    private final VpLogService vpLogService;
    private final GameCalculationService gameCalculationService;

    /**
     * 초기 광산 배치 (게임 시작 전)
     * 순서: 1→2→3→4→4→3→2→1 (snake draft)
     */
    @Transactional
    public PlaceInitialMineResponse placeInitialMine(UUID gameId, PlaceInitialMineRequest request) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("게임을 찾을 수 없습니다: " + gameId));

        // 1. 게임 페이즈 확인
        if (!game.isInSetupPhase()) {
            throw new IllegalStateException("초기 광산 배치 단계가 아닙니다. 현재 페이즈: " + game.getGamePhase());
        }

        // 2. 현재 배치할 좌석 확인
        int currentSetupSeatNo = game.getCurrentSetupSeatNo();
        GameSeat currentSeat = gameSeatRepository.findByGameIdAndSeatNo(gameId, currentSetupSeatNo)
                .orElseThrow(() -> new IllegalStateException("좌석을 찾을 수 없습니다: " + currentSetupSeatNo));

        // 3. 플레이어 검증
        if (!currentSeat.getPlayerId().equals(request.playerId())) {
            throw new IllegalArgumentException(
                    "현재 배치 순서가 아닙니다. 현재 배치 순서: 좌석 " + currentSetupSeatNo
            );
        }

        // 4. 헥스 유효성 및 행성 타입 확인 (game_hex에서 조회)
        GameHex hex = gameHexRepository.findByGameIdAndHexQAndHexR(gameId, request.hexQ(), request.hexR())
                .orElseThrow(() -> new IllegalArgumentException(
                        "유효하지 않은 좌표입니다: (" + request.hexQ() + ", " + request.hexR() + ")"
                ));

        // 5. 헥스 점유 확인
        if (gameBuildingRepository.existsByGameIdAndHexQAndHexR(gameId, request.hexQ(), request.hexR())) {
            throw new IllegalArgumentException(
                    "이미 건물이 있는 위치입니다: (" + request.hexQ() + ", " + request.hexR() + ")"
            );
        }

        // 6. 행성 타입 검증 (종족의 홈 플래닛과 일치해야 함)
        FactionType faction = currentSeat.getFactionType();
        PlanetType homePlanet = faction.getHomePlanet();
        PlanetType hexPlanet = hex.getPlanetType();

        // BE에서 조회한 행성 타입과 홈 플래닛 비교
        if (!hexPlanet.equals(homePlanet)) {
            throw new IllegalArgumentException(
                    "종족의 홈 플래닛에만 광산을 배치할 수 있습니다. " +
                    "종족: " + faction.getDisplayNameKo() + ", 홈 플래닛: " + homePlanet.getDisplayNameKo() +
                    ", 선택된 행성: " + hexPlanet.getDisplayNameKo()
            );
        }

        // 6. 플레이어 상태 조회 및 재고 확인
        GamePlayerState playerState = gamePlayerStateRepository.findByGameIdAndPlayerId(gameId, request.playerId())
                .orElseThrow(() -> new IllegalStateException("플레이어 상태를 찾을 수 없습니다: " + request.playerId()));

        boolean isSetupPI = faction.isSetupPlanetaryInstitute();
        if (isSetupPI) {
            if (playerState.getStockPlanetaryInstitute() <= 0) {
                throw new IllegalStateException("행성 의회 재고가 없습니다.");
            }
        } else {
            if (playerState.getStockMine() <= 0) {
                throw new IllegalStateException("광산 재고가 없습니다.");
            }
        }

        // 7. 건물 배치 (종족에 따라 광산 또는 행성 의회)
        BuildingType buildingType = isSetupPI ? BuildingType.PLANETARY_INSTITUTE : BuildingType.MINE;
        GameBuilding mine = GameBuilding.place(
                gameId,
                request.playerId(),
                request.hexQ(),
                request.hexR(),
                buildingType
        );
        gameBuildingRepository.save(mine);

        // 8. 재고 감소
        if (isSetupPI) {
            playerState.decreaseStockPlanetaryInstitute();
        } else {
            playerState.decreaseStockMine();
        }
        gamePlayerStateRepository.save(playerState);

        // 9. 현재 플레이어 타이머 종료 + 다음 배치 턴으로
        actionService.stopTurnTimer(gameId, request.playerId());
        game.nextMinePlacement();
        gameRepository.save(game);
        // 다음 배치 플레이어 타이머 시작
        if (game.isInSetupPhase()) {
            actionService.startTurnTimerBySeatNo(gameId, game.getCurrentSetupSeatNo());
        }

        log.info("초기 광산 배치 완료: 게임={}, 좌석={}, 위치=({},{}), 남은재고={}",
                gameId, currentSetupSeatNo, request.hexQ(), request.hexR(), playerState.getStockMine());

        // 10. WebSocket 브로드캐스트 - 광산 배치 알림
        Integer nextSeatNoForBroadcast = game.isInSetupPhase() ? game.getCurrentSetupSeatNo() : null;
        webSocketService.broadcastMinePlaced(
                gameId,
                request.playerId(),
                currentSetupSeatNo,
                request.hexQ(),
                request.hexR(),
                nextSeatNoForBroadcast,
                game.getGamePhase()
        );

        // 11. 광산 배치 완료 여부 확인 (부스터 선택 단계로 진입)
        //     수입 적용은 부스터 선택 완료 후에 진행됨
        boolean isSetupComplete = !game.isInSetupPhase();
        if (isSetupComplete) {
            log.info("초기 광산 배치 완료. 부스터 선택 단계로 진입: 게임={}", gameId);
        }
        Integer nextSeatNo = isSetupComplete ? null : game.getCurrentSetupSeatNo();
        String nextPhase = game.getGamePhase();

        return new PlaceInitialMineResponse(
                gameId,
                mine.getId(),
                request.hexQ(),
                request.hexR(),
                currentSetupSeatNo,
                playerState.getStockMine(),
                isSetupComplete,
                nextSeatNo,
                nextPhase,
                buildingType.name()
        );
    }

    /**
     * 게임 내 모든 건물 조회
     */
    @Transactional(readOnly = true)
    public List<GameBuilding> getBuildingsByGame(UUID gameId) {
        return gameBuildingRepository.findByGameId(gameId);
    }

    /**
     * 특정 플레이어의 건물 조회
     */
    @Transactional(readOnly = true)
    public List<GameBuilding> getBuildingsByPlayer(UUID gameId, UUID playerId) {
        return gameBuildingRepository.findByGameIdAndPlayerId(gameId, playerId);
    }

    /**
     * 특정 플레이어의 특정 타입 건물 수
     */
    @Transactional(readOnly = true)
    public int countBuildingsByType(UUID gameId, UUID playerId, BuildingType buildingType) {
        return gameBuildingRepository.countByGameIdAndPlayerIdAndBuildingType(gameId, playerId, buildingType);
    }

    /**
     * 건물 업그레이드 공용 코어 로직
     * - 일반 업그레이드(upgradeBuildingInPlay)와 함대 액션 업그레이드(FleetShipActionService)에서 공유
     * - 비용 지불은 호출자가 처리, 여기서는 재고/건물/기술타일/라운드점수/리치 처리
     *
     * @param game          게임 엔티티
     * @param playerId      플레이어 ID
     * @param building      업그레이드 대상 건물
     * @param targetType    목표 건물 타입
     * @param academyType   아카데미 종류 (ACADEMY가 아니면 null)
     * @param techTileCode  기술 타일 코드 (선택, null이면 스킵)
     * @param techTrackCode 기술 트랙 코드 (선택)
     * @param coveredTileCode 덮을 기본 타일 코드 (고급 타일 시)
     * @param actionType    저장할 액션 타입 (UPGRADE_BUILDING 또는 FLEET_SHIP_ACTION)
     * @param actionData    저장할 액션 데이터 JSON
     * @param awardRoundScoring 라운드 점수 부여 여부
     * @return 실패 시 에러 메시지, 성공 시 null
     */
    String upgradeCore(
            Game game, UUID playerId, GameBuilding building,
            BuildingType targetType, String academyType,
            String techTileCode, String techTrackCode, String coveredTileCode,
            ActionType actionType, String actionData, boolean awardRoundScoring,
            Integer lostPlanetHexQ, Integer lostPlanetHexR,
            Integer mineHexQ, Integer mineHexR, Integer mineQicUsed) {

        UUID gameId = game.getId();
        BuildingType fromType = building.getBuildingType();

        GamePlayerState playerState = gamePlayerStateRepository.findByGameIdAndPlayerId(gameId, playerId)
                .orElseThrow(() -> new IllegalStateException("플레이어 상태를 찾을 수 없습니다"));

        // 재고 감소 (목표 건물)
        switch (targetType) {
            case TRADING_STATION -> playerState.decreaseStockTradingStation();
            case RESEARCH_LAB -> playerState.decreaseStockResearchLab();
            case PLANETARY_INSTITUTE -> playerState.decreaseStockPlanetaryInstitute();
            case ACADEMY -> playerState.decreaseStockAcademy();
            default -> { return "업그레이드 불가 건물 타입"; }
        }

        // 이전 건물 타입 재고 반환
        switch (fromType) {
            case MINE -> playerState.addMineToStock();
            case TRADING_STATION -> playerState.addTradingStationToStock();
            case RESEARCH_LAB -> playerState.addResearchLabToStock();
            default -> {}
        }

        gamePlayerStateRepository.save(playerState);

        // 건물 업그레이드
        if (targetType == BuildingType.ACADEMY && academyType != null) {
            var acType = com.gaiaproject.domain.enumtype.building.AcademyType.valueOf(academyType);
            building.upgradeToAcademy(acType);
            if (acType == com.gaiaproject.domain.enumtype.building.AcademyType.QIC
                    && playerState.getFactionType() == FactionType.GLEENS) {
                playerState.setGleensHasQicAcademy(true);
                gamePlayerStateRepository.save(playerState);
            }
        } else {
            building.upgrade(targetType);
        }
        gameBuildingRepository.save(building);

        log.info("건물 업그레이드: game={}, player={}, ({},{}) {}→{}", gameId, playerId, building.getHexQ(), building.getHexR(), fromType, targetType);

        // PASSIVE: ADV_TILE_17 - 교역소 건설 시 3VP
        if (targetType == BuildingType.TRADING_STATION && gameCalculationService.hasActiveTechTile(gameId, playerId, "ADV_TILE_17")) {
            playerState.addVP(3);
            vpLogService.logVp(gameId, playerId, VpCategory.ADV_TECH_TILE, 3, null, "ADV_TILE_17 교역소 건설 3VP");
            gamePlayerStateRepository.save(playerState);
        }

        // 라운드 점수 타일 적용
        if (awardRoundScoring && game.getCurrentRound() != null) {
            int round = game.getCurrentRound();
            switch (targetType) {
                case TRADING_STATION -> roundScoringService.award(gameId, round, playerState, RoundScoringEvent.TRADING_STATION_BUILT, 1);
                case RESEARCH_LAB -> roundScoringService.award(gameId, round, playerState, RoundScoringEvent.RESEARCH_LAB_BUILT, 1);
                case PLANETARY_INSTITUTE -> roundScoringService.award(gameId, round, playerState, RoundScoringEvent.PLANETARY_INSTITUTE_BUILT, 1);
                case ACADEMY -> roundScoringService.award(gameId, round, playerState, RoundScoringEvent.ACADEMY_BUILT, 1);
                default -> {}
            }
        }

        // 글린 PI 건설 시 글린 전용 연방 토큰 즉시 지급
        if (targetType == BuildingType.PLANETARY_INSTITUTE
                && playerState.getFactionType() == FactionType.GLEENS) {
            var gleensFedToken = com.gaiaproject.domain.entity.player.GamePlayerFederationToken.builder()
                    .gameId(gameId)
                    .playerId(playerId)
                    .federationTileType(com.gaiaproject.domain.enumtype.federation.FederationTileType.GLEENS_FEDERATION)
                    .build();
            federationTokenRepository.save(gleensFedToken);
            playerState.addCredit(2);
            playerState.addOre(1);
            playerState.addKnowledge(1);
            int round = game.getCurrentRound() != null ? game.getCurrentRound() : 1;
            roundScoringService.award(gameId, round, playerState,
                    com.gaiaproject.domain.enumtype.rounds.RoundScoringEvent.FEDERATION_FORMED, 1);
            log.info("[GLEENS PI] 전용 연방 토큰 지급 + 즉시 보상 2c 1o 1k + 라운드점수: player={}", playerId);
        }

        gamePlayerStateRepository.save(playerState);

        // 기술 타일 획득 (연구소/아카데미/스페이스자이언트 의회)
        com.gaiaproject.dto.TileAcquisitionResult tileResult = null;
        boolean isTileEligible = targetType == BuildingType.RESEARCH_LAB
                || targetType == BuildingType.ACADEMY
                || (targetType == BuildingType.PLANETARY_INSTITUTE
                    && playerState.getFactionType() == FactionType.SPACE_GIANTS);
        if (isTileEligible && techTileCode != null && !techTileCode.isBlank()) {
            log.info("[UPGRADE_CORE] acquireTile: tile={}, track={}, mineHex=({},{}), lpHex=({},{})",
                    techTileCode, techTrackCode, mineHexQ, mineHexR, lostPlanetHexQ, lostPlanetHexR);
            tileResult = techTileService.acquireTileForBuilding(
                    gameId, playerId,
                    techTileCode, techTrackCode,
                    game.getEconomyTrackOption(), coveredTileCode);
        }

        // 검은행성 배치 (거리 5단계 진입 시, 리치 전에 처리)
        if (lostPlanetHexQ != null && lostPlanetHexR != null) {
            PlaceMinePlayResponse lpResult = placeLostPlanetInternal(game, playerId, lostPlanetHexQ, lostPlanetHexR);
            if (!lpResult.success()) {
                return lpResult.message();
            }
        }

        // 액션 저장 (턴 진행은 리치 해소 후)
        actionService.saveActionOnly(gameId, playerId, actionType, actionData);

        // 2삽 광산 인라인 배치 (좌표가 제공된 경우 DEFERRED 없이 즉시 처리)
        if (tileResult != null && tileResult.needsMine() && mineHexQ != null && mineHexR != null) {
            PlaceMinePlayResponse mineResult = placeMineInPlayInternal(game, playerId, mineHexQ, mineHexR,
                    mineQicUsed != null ? mineQicUsed : 0, false, 2, true);
            if (!mineResult.success()) {
                log.warn("[UPGRADE_MINE_INLINE] 2삽 광산 배치 실패: {}", mineResult.message());
            }
        }

        // 후속 액션 판단 (기술타일 획득 결과 기반)
        String followUpType = null;
        String followUpData = null;

        // 2삽 광산: 좌표 제공 → MINE_LEECH로 리치 체인, 미제공 → DEFERRED
        boolean mineHandledInline = tileResult != null && tileResult.needsMine() && mineHexQ != null && mineHexR != null;

        if (mineHandledInline && lostPlanetHexQ != null && lostPlanetHexR != null) {
            // 2삽 광산 + 검은행성 모두 인라인: 업그레이드 리치 → 광산 리치 → 검은행성 리치
            followUpType = "MINE_LEECH";
            followUpData = String.format("{\"hexQ\":%d,\"hexR\":%d,\"playerId\":\"%s\",\"nextFollowUp\":\"LOST_PLANET_LEECH\",\"lpHexQ\":%d,\"lpHexR\":%d}",
                    mineHexQ, mineHexR, playerId, lostPlanetHexQ, lostPlanetHexR);
        } else if (mineHandledInline) {
            // 2삽 광산만 인라인: 업그레이드 리치 → 광산 리치
            followUpType = "MINE_LEECH";
            followUpData = String.format("{\"hexQ\":%d,\"hexR\":%d,\"playerId\":\"%s\"}", mineHexQ, mineHexR, playerId);
        } else if (lostPlanetHexQ != null && lostPlanetHexR != null) {
            followUpType = "LOST_PLANET_LEECH";
            followUpData = String.format("{\"hexQ\":%d,\"hexR\":%d,\"playerId\":\"%s\"}", lostPlanetHexQ, lostPlanetHexR, playerId);
        } else if (tileResult != null && tileResult.needsMine()) {
            // 2삽 좌표 미제공 → 기존 DEFERRED 폴백
            followUpType = "PLACE_MINE_TERRAFORM_2";
            followUpData = String.format("{\"terraformDiscount\":2,\"triggerPlayerId\":\"%s\"}", playerId);
        } else if (tileResult != null && tileResult.needsLostPlanet()) {
            followUpType = "PLACE_LOST_PLANET";
            followUpData = String.format("{\"triggerPlayerId\":\"%s\"}", playerId);
        }

        // 파워 리치 처리
        List<GameBuilding> allBuildings = gameBuildingRepository.findByGameId(gameId);
        powerLeechService.createBatchAndProcess(game, playerId, building.getHexQ(), building.getHexR(), targetType, allBuildings, followUpType, followUpData);

        return null; // 성공
    }

    /** 헥스 거리 계산 (flat-top axial) */
    private int hexDistance(int q1, int r1, int q2, int r2) {
        int dq = q2 - q1, dr = r2 - r1;
        return (Math.abs(dq) + Math.abs(dr) + Math.abs(dq + dr)) / 2;
    }

    /** 건물 파워 값 (리치 계산용) */
    private int buildingPowerValue(BuildingType type) {
        return switch (type) {
            case MINE -> 1;
            case TRADING_STATION, RESEARCH_LAB -> 2;
            case PLANETARY_INSTITUTE, ACADEMY -> 3;
            default -> 0;
        };
    }

    /**
     * 테라포밍 광석 비용 계산
     * - 소행성/미행성 종족: 링 행성은 항상 1단계
     * - 일반 종족: 테라포밍 링 순환 최단 거리
     * - 단계당 광석: techTerraforming 0~2 → 3, 3 → 2, 4~5 → 1
     */
    private int calcTerraformingOre(FactionType faction, PlanetType targetPlanet, int techTerraforming, int terraformDiscount) {
        // 원시행성(LOST_PLANET)/검은행성(BLACK_PLANET): 홈 종족 포함 항상 3삽 비용
        if (targetPlanet == PlanetType.LOST_PLANET || targetPlanet == PlanetType.BLACK_PLANET) {
            int orePerStep = techTerraforming <= 1 ? 3 : techTerraforming == 2 ? 2 : 1;
            int effectiveSteps = Math.max(0, 3 - terraformDiscount);
            return effectiveSteps * orePerStep;
        }

        // 소행성(ASTEROIDS): 가이아포머 소각 경로로 처리 → 테라포밍 광석 없음
        if (targetPlanet == PlanetType.ASTEROIDS) {
            return 0;
        }

        PlanetType homePlanet = faction.getHomePlanet();
        if (homePlanet == targetPlanet) return 0;

        int steps;
        if (homePlanet == PlanetType.ASTEROIDS || homePlanet == PlanetType.LOST_PLANET) {
            // 소행성/미행성 종족: 링 행성은 항상 1단계
            steps = isRingPlanet(targetPlanet) ? 1 : 0;
        } else {
            steps = terraformRingDistance(homePlanet, targetPlanet);
        }

        steps = Math.max(0, steps - terraformDiscount);

        if (steps <= 0) return 0;
        // 레벨 0~1: 3광석, 레벨 2: 2광석, 레벨 3~5: 1광석
        int orePerStep = techTerraforming <= 1 ? 3 : techTerraforming == 2 ? 2 : 1;
        return steps * orePerStep;
    }

    private static final PlanetType[] TERRAFORM_RING = {
        PlanetType.TERRA, PlanetType.VOLCANIC, PlanetType.OXIDE,
        PlanetType.DESERT, PlanetType.SWAMP, PlanetType.TITANIUM, PlanetType.ICE
    };

    private boolean isRingPlanet(PlanetType p) {
        for (PlanetType r : TERRAFORM_RING) if (r == p) return true;
        return false;
    }

    private int terraformRingDistance(PlanetType from, PlanetType to) {
        int a = -1, b = -1;
        for (int i = 0; i < TERRAFORM_RING.length; i++) {
            if (TERRAFORM_RING[i] == from) a = i;
            if (TERRAFORM_RING[i] == to) b = i;
        }
        if (a == -1 || b == -1) return 0;
        int diff = Math.abs(a - b);
        return Math.min(diff, TERRAFORM_RING.length - diff);
    }

    /** 의회 건설 여부 확인 (재고 0 = 이미 건설) */
    private boolean hasPlanetaryInstitute(GamePlayerState ps) {
        return ps.getStockPlanetaryInstitute() == 0;
    }

    /** 새 섹터 진출 여부: 해당 섹터에 내 건물(가이아포머/우주정거장/방금 놓은 건물 제외)이 없으면 true. 1헥스 섹터는 실제 섹터가 아니므로 항상 false. */
    private boolean isNewSectorForPlayer(UUID gameId, UUID playerId, String sectorId, int newHexQ, int newHexR) {
        if (!GameHex.isRealSector(sectorId)) return false;
        List<GameBuilding> myBuildings = gameBuildingRepository.findByGameIdAndPlayerId(gameId, playerId);
        for (GameBuilding b : myBuildings) {
            if (b.getHexQ() == newHexQ && b.getHexR() == newHexR) continue; // 방금 놓은 건물 제외
            if (b.getBuildingType() == BuildingType.GAIAFORMER) continue;
            if (b.getBuildingType() == BuildingType.SPACE_STATION) continue;
            if (b.isLantidsMine()) continue; // 기생 광산은 섹터 점유로 보지 않음
            GameHex bHex = gameHexRepository.findByGameIdAndHexQAndHexR(gameId, b.getHexQ(), b.getHexR()).orElse(null);
            if (bHex != null && sectorId.equals(bHex.getSectorId())) return false;
        }
        return true;
    }

    /**
     * 테라포밍 원시 단계 수 (할인 없이, 점수 계산용)
     */
    private int calcRawTerraformingSteps(FactionType faction, PlanetType targetPlanet) {
        if (targetPlanet == PlanetType.LOST_PLANET || targetPlanet == PlanetType.BLACK_PLANET) return 3;
        if (targetPlanet == PlanetType.ASTEROIDS || targetPlanet == null) return 0;
        PlanetType homePlanet = faction.getHomePlanet();
        if (homePlanet == targetPlanet) return 0;
        if (homePlanet == PlanetType.ASTEROIDS || homePlanet == PlanetType.LOST_PLANET) {
            return isRingPlanet(targetPlanet) ? 1 : 0;
        }
        return terraformRingDistance(homePlanet, targetPlanet);
    }

    /**
     * 해당 플레이어에게 처음 개척하는 행성 종류인지 확인 (ROUND_TILE_NEW_PLANET_TYPE 용)
     */
    private boolean isNewPlanetTypeForPlayer(UUID gameId, UUID playerId, PlanetType newPlanetType, int newHexQ, int newHexR) {
        if (newPlanetType == null
                || newPlanetType == PlanetType.EMPTY
                || newPlanetType == PlanetType.TRANSDIM) {
            return false;
        }
        List<GameBuilding> existing = gameBuildingRepository.findByGameIdAndPlayerId(gameId, playerId);
        for (GameBuilding b : existing) {
            if (b.getHexQ() == newHexQ && b.getHexR() == newHexR) continue; // 방금 놓은 건물 제외
            if (b.getBuildingType() == BuildingType.GAIAFORMER) continue;
            if (b.isLantidsMine()) continue; // 란티다 기생은 행성 종류에 포함하지 않음
            GameHex bHex = gameHexRepository.findByGameIdAndHexQAndHexR(gameId, b.getHexQ(), b.getHexR()).orElse(null);
            if (bHex != null && bHex.getPlanetType() == newPlanetType) return false;
        }
        return true;
    }


    /**
     * 광산 건설 내부 로직 (리치/액션저장 제외 — upgradeCore 인라인용)
     * placeMineInPlay를 PlaceMinePlayRequest로 래핑하여 호출하되, 리치는 호출자가 체인으로 처리
     */
    PlaceMinePlayResponse placeMineInPlayInternal(Game game, UUID playerId, int hexQ, int hexR,
                                                   int qicUsed, boolean gaiaformerUsed, int terraformDiscount, boolean freeMine) {
        PlaceMinePlayRequest req = new PlaceMinePlayRequest(playerId, hexQ, hexR, qicUsed, gaiaformerUsed, terraformDiscount, freeMine, false);

        UUID gameId = game.getId();
        GameHex hex = gameHexRepository.findByGameIdAndHexQAndHexR(gameId, hexQ, hexR).orElse(null);
        if (hex == null) return PlaceMinePlayResponse.fail(gameId, "유효하지 않은 좌표입니다");

        GameBuilding existingBuilding = gameBuildingRepository
                .findFirstByGameIdAndHexQAndHexRAndIsLantidsMine(gameId, hexQ, hexR, false).orElse(null);
        boolean isGaiaformerReturn = existingBuilding != null
                && existingBuilding.getBuildingType() == BuildingType.GAIAFORMER
                && existingBuilding.getPlayerId().equals(playerId)
                && hex.getPlanetType() == PlanetType.GAIA;

        GamePlayerState playerState = gamePlayerStateRepository.findByGameIdAndPlayerId(gameId, playerId)
                .orElseThrow(() -> new IllegalStateException("플레이어 상태를 찾을 수 없습니다"));

        // 비용 처리 (freeMine이므로 테라포밍 비용만)
        if (freeMine && terraformDiscount > 0) {
            int terraformingOre = calcTerraformingOre(playerState.getFactionType(), hex.getPlanetType(),
                    playerState.getTechTerraforming(), terraformDiscount);
            if (terraformingOre > 0) playerState.spendOre(terraformingOre);
        }
        if (qicUsed > 0) playerState.spendQic(qicUsed);

        // 건물 배치
        playerState.decreaseStockMine();
        gamePlayerStateRepository.save(playerState);

        GameBuilding mine = GameBuilding.place(gameId, playerId, hexQ, hexR, BuildingType.MINE);
        gameBuildingRepository.save(mine);

        // 라운드 점수
        if (game.getCurrentRound() != null) {
            roundScoringService.award(gameId, game.getCurrentRound(), playerState, RoundScoringEvent.MINE_PLACED, 1);
            if (!isGaiaformerReturn && hex.getPlanetType() != PlanetType.GAIA && !gaiaformerUsed) {
                int steps = calcRawTerraformingSteps(playerState.getFactionType(), hex.getPlanetType());
                if (steps > 0) roundScoringService.award(gameId, game.getCurrentRound(), playerState, RoundScoringEvent.TERRAFORM_STEP, steps);
            }
            if (isNewSectorForPlayer(gameId, playerId, hex.getSectorId(), hexQ, hexR))
                roundScoringService.award(gameId, game.getCurrentRound(), playerState, RoundScoringEvent.NEW_SECTOR_ENTERED, 1);
            if (isNewPlanetTypeForPlayer(gameId, playerId, hex.getPlanetType(), hexQ, hexR))
                roundScoringService.award(gameId, game.getCurrentRound(), playerState, RoundScoringEvent.NEW_PLANET_TYPE_COLONIZED, 1);
        }

        // 패시브 타일
        if (gameCalculationService.hasActiveTechTile(gameId, playerId, "ADV_TILE_16")) {
            playerState.addVP(3);
            vpLogService.logVp(gameId, playerId, VpCategory.ADV_TECH_TILE, 3, null, "ADV_TILE_16 광산 건설 3VP");
        }
        if (gameCalculationService.hasActiveTechTile(gameId, playerId, "ADV_TILE_10")) {
            int steps = calcRawTerraformingSteps(playerState.getFactionType(), hex.getPlanetType());
            if (steps > 0) {
                playerState.addVP(steps * 2);
                vpLogService.logVp(gameId, playerId, VpCategory.ADV_TECH_TILE, steps * 2, null, "ADV_TILE_10 테라포밍 " + steps + "삽 × 2VP");
            }
        }
        gamePlayerStateRepository.save(playerState);

        // 연방 자동 편입
        federationFormService.autoJoinFederation(gameId, playerId, hexQ, hexR);

        // 액션 저장
        actionService.saveActionOnly(gameId, playerId, ActionType.PLACE_MINE,
                String.format("{\"hexQ\":%d,\"hexR\":%d,\"type\":\"INLINE_MINE\"}", hexQ, hexR));

        log.info("[INLINE_MINE] 2삽 광산 인라인 배치: game={}, player={}, ({},{})", gameId, playerId, hexQ, hexR);
        return PlaceMinePlayResponse.success(gameId, hexQ, hexR, 0);
    }

    /**
     * 검은행성(BLACK_PLANET) 배치 — 거리 트랙 5단계 도달 보상
     * - EMPTY 헥스에만 배치 가능
     * - 연방 토큰 위에 불가
     * - 광산 재고 감소 없음
     * - 광산 취급 (파워값 1, 연방 참여, 리치 발생, 라운드 점수)
     * - 해당 헥스의 planetType을 BLACK_PLANET으로 변경
     */
    @Transactional
    public PlaceMinePlayResponse placeLostPlanet(UUID gameId, UUID playerId, int hexQ, int hexR, int qicUsed) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("게임을 찾을 수 없습니다"));
        PlaceMinePlayResponse result = placeLostPlanetInternal(game, playerId, hexQ, hexR, qicUsed);
        if (!result.success()) return result;

        // 액션 저장 (턴 유지 — 리치 해소 후 턴 진행)
        actionService.saveActionOnly(gameId, playerId, ActionType.PLACE_MINE,
                String.format("{\"hexQ\":%d,\"hexR\":%d,\"type\":\"LOST_PLANET\"}", hexQ, hexR));

        // 리치 처리
        List<GameBuilding> allBuildings = gameBuildingRepository.findByGameId(gameId);
        powerLeechService.createBatchAndProcess(game, playerId, hexQ, hexR, BuildingType.LOST_PLANET_MINE, allBuildings, null, null);

        return result;
    }

    /** 검은행성 배치 핵심 로직 (리치/액션 저장 제외) */
    private PlaceMinePlayResponse placeLostPlanetInternal(Game game, UUID playerId, int hexQ, int hexR) {
        return placeLostPlanetInternal(game, playerId, hexQ, hexR, 0);
    }

    private PlaceMinePlayResponse placeLostPlanetInternal(Game game, UUID playerId, int hexQ, int hexR, int qicUsed) {
        UUID gameId = game.getId();

        GamePlayerState ps = gamePlayerStateRepository.findByGameIdAndPlayerId(gameId, playerId)
                .orElseThrow(() -> new IllegalStateException("플레이어 상태를 찾을 수 없습니다"));

        // 헥스 검증: EMPTY만
        GameHex hex = gameHexRepository.findByGameIdAndHexQAndHexR(gameId, hexQ, hexR)
                .orElseThrow(() -> new IllegalArgumentException("헥스를 찾을 수 없습니다: (" + hexQ + "," + hexR + ")"));
        if (hex.getPlanetType() != PlanetType.EMPTY) {
            return PlaceMinePlayResponse.fail(gameId, "빈 헥스에만 검은행성을 배치할 수 있습니다");
        }

        // 기존 건물 없는지 확인
        if (gameBuildingRepository.findFirstByGameIdAndHexQAndHexRAndIsLantidsMine(gameId, hexQ, hexR, false).isPresent()) {
            return PlaceMinePlayResponse.fail(gameId, "이미 건물이 있는 헥스입니다");
        }

        // 연방 토큰 위 불가
        var allGroups = federationFormService.getFederationGroups(gameId);
        boolean hasFedToken = allGroups.stream()
                .flatMap(g -> g.tokenHexes().stream())
                .anyMatch(t -> t[0] == hexQ && t[1] == hexR);
        if (hasFedToken) {
            return PlaceMinePlayResponse.fail(gameId, "연방 토큰 위에 검은행성을 배치할 수 없습니다");
        }

        // 항법 거리 체크 (거리 5단계 도달 시점 → 기본 거리 4)
        int navRange = switch (ps.getTechNavigation()) {
            case 0 -> 1; case 1 -> 1; case 2 -> 2; case 3 -> 2; case 4 -> 3; default -> 4;
        };
        if (gameCalculationService.hasActiveTechTile(gameId, playerId, "BASIC_EXP_TILE_1")) {
            navRange += 1;
        }
        int finalNavRange = navRange + qicUsed * 2;
        List<GameBuilding> myBuildings = gameBuildingRepository.findByGameIdAndPlayerId(gameId, playerId);
        boolean inRange = myBuildings.stream().anyMatch(b ->
                hexDistance(b.getHexQ(), b.getHexR(), hexQ, hexR) <= finalNavRange);
        if (!inRange) {
            return PlaceMinePlayResponse.fail(gameId, "항법 거리 밖입니다 (현재 거리: " + finalNavRange + ")");
        }
        // QIC 소모
        if (qicUsed > 0) {
            if (ps.getQic() < qicUsed) {
                return PlaceMinePlayResponse.fail(gameId, "QIC가 부족합니다");
            }
            ps.spendQic(qicUsed);
        }

        // 헥스 planetType 변경 → BLACK_PLANET (검은행성)
        hex.setPlanetType(PlanetType.BLACK_PLANET);
        gameHexRepository.save(hex);

        // 건물 배치 (LOST_PLANET_MINE)
        GameBuilding building = GameBuilding.place(gameId, playerId, hexQ, hexR, BuildingType.LOST_PLANET_MINE);
        gameBuildingRepository.save(building);

        gamePlayerStateRepository.save(ps);

        // 기오덴 PI: 새 행성 타입 개척 시 +3 지식
        if (ps.getFactionType() == FactionType.GEODENS
                && hasPlanetaryInstitute(ps)
                && isNewPlanetTypeForPlayer(gameId, playerId, PlanetType.BLACK_PLANET, hexQ, hexR)) {
            ps.addKnowledge(3);
            gamePlayerStateRepository.save(ps);
            log.info("[GEODENS PI] 검은행성 새 행성 타입 지식 +3: player={}", playerId);
        }

        // 라운드 점수
        if (game.getCurrentRound() != null) {
            roundScoringService.award(gameId, game.getCurrentRound(), ps, RoundScoringEvent.MINE_PLACED, 1);
            // 새 행성 종류
            if (isNewPlanetTypeForPlayer(gameId, playerId, PlanetType.BLACK_PLANET, hexQ, hexR)) {
                roundScoringService.award(gameId, game.getCurrentRound(), ps, RoundScoringEvent.NEW_PLANET_TYPE_COLONIZED, 1);
            }
            // 새 섹터 진출
            if (hex.getSectorId() != null && isNewSectorForPlayer(gameId, playerId, hex.getSectorId(), hexQ, hexR)) {
                roundScoringService.award(gameId, game.getCurrentRound(), ps, RoundScoringEvent.NEW_SECTOR_ENTERED, 1);
            }
        }

        // PASSIVE: ADV_TILE_16 - 광산 건설 시 3VP
        if (gameCalculationService.hasActiveTechTile(gameId, playerId, "ADV_TILE_16")) {
            ps.addVP(3);
            vpLogService.logVp(gameId, playerId, VpCategory.ADV_TECH_TILE, 3, null, "ADV_TILE_16 검은행성 광산 3VP");
        }

        gamePlayerStateRepository.save(ps);

        // 연방 자동 편입
        federationFormService.autoJoinFederation(gameId, playerId, hexQ, hexR);

        log.info("[LOST_PLANET] 검은행성 배치: game={}, player={}, hex=({},{})", gameId, playerId, hexQ, hexR);
        return PlaceMinePlayResponse.success(gameId, hexQ, hexR, 0);
    }

}
