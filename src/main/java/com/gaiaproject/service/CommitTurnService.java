package com.gaiaproject.service;

import com.gaiaproject.domain.entity.building.GameBuilding;
import com.gaiaproject.domain.entity.game.Game;
import com.gaiaproject.domain.entity.game.GamePowerActionUsage;
import com.gaiaproject.domain.entity.map.GameHex;
import com.gaiaproject.domain.entity.player.GamePlayerArtifact;
import com.gaiaproject.domain.entity.player.GamePlayerFederationToken;
import com.gaiaproject.domain.entity.player.GamePlayerFleetProbe;
import com.gaiaproject.domain.entity.player.GamePlayerState;
import com.gaiaproject.domain.entity.player.GamePlayerTechTile;
import com.gaiaproject.domain.enumtype.federation.FederationTileType;
import com.gaiaproject.domain.enumtype.player.FactionType;
import com.gaiaproject.domain.enumtype.action.ActionType;
import com.gaiaproject.domain.enumtype.action.PowerActionType;
import com.gaiaproject.domain.enumtype.action.VpCategory;
import com.gaiaproject.domain.enumtype.building.BuildingType;
import com.gaiaproject.domain.enumtype.player.PlanetType;
import com.gaiaproject.dto.request.CommitTurnRequest;
import com.gaiaproject.dto.request.PlayerStateSnapshot;
import com.gaiaproject.dto.response.CommitTurnResponse;
import com.gaiaproject.dto.response.GameSnapshot;
import com.gaiaproject.repository.building.GameBuildingRepository;
import com.gaiaproject.repository.game.GameRepository;
import com.gaiaproject.repository.artifact.GameArtifactOfferRepository;
import com.gaiaproject.repository.game.GamePowerActionUsageRepository;
import com.gaiaproject.repository.leech.GameLeechOfferRepository;
import com.gaiaproject.repository.map.GameHexRepository;
import com.gaiaproject.repository.player.GamePlayerArtifactRepository;
import com.gaiaproject.repository.player.GamePlayerFederationTokenRepository;
import com.gaiaproject.repository.player.GamePlayerFleetProbeRepository;
import com.gaiaproject.repository.player.GamePlayerStateRepository;
import com.gaiaproject.repository.tech.GameAdvTechOfferRepository;
import com.gaiaproject.repository.tech.GamePlayerTechTileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 턴 확정 — C안 commit-turn.
 *
 * FE가 계산한 최종 상태를 받아 DB에 덮어쓰기.
 * 규칙 검증/재계산 없음. 신뢰 모델: FE가 이미 규칙을 적용한 결과이므로 그대로 저장.
 *
 * 책임:
 * 1. 권한 체크 (내 턴인가)
 * 2. PlayerState 덮어쓰기 (PlayerStateSnapshot의 non-null 필드만)
 * 3. 건물 INSERT / UPDATE
 * 4. 헥스 planet_type UPDATE (테라포밍/블랙행성)
 * 5. 기술타일 INSERT + 커버 UPDATE
 * 6. 액션 로그 / VP 로그 INSERT
 * 7. 리치 오퍼 생성 + WS 브로드캐스트
 * 8. 리치 없으면 턴 즉시 진행, 있으면 리치 해소 후 PowerLeechService가 진행
 *
 * 주의: 연방 변경(newFederationGroups/flippedFederationTokens)은 Phase 6에서 처리 예정.
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class CommitTurnService {

    private final GameRepository gameRepository;
    private final GamePlayerStateRepository playerStateRepository;
    private final GameBuildingRepository buildingRepository;
    private final GameHexRepository hexRepository;
    private final GamePlayerTechTileRepository techTileRepository;
    private final GameLeechOfferRepository leechOfferRepository;
    private final GamePowerActionUsageRepository powerActionUsageRepository;
    private final GameArtifactOfferRepository artifactOfferRepository;
    private final ActionService actionService;
    private final VpLogService vpLogService;
    private final GameWebSocketService webSocketService;
    private final PowerLeechService powerLeechService;
    private final GamePlayerArtifactRepository playerArtifactRepository;
    private final GamePlayerFederationTokenRepository playerFederationTokenRepository;
    private final GamePlayerFleetProbeRepository fleetProbeRepository;
    private final GameAdvTechOfferRepository advTechOfferRepository;
    private final FederationFormService federationFormService;
    private final TechTileService techTileService;
    private final GameSnapshotService gameSnapshotService;

    public CommitTurnResponse commit(UUID gameId, CommitTurnRequest req) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("게임을 찾을 수 없습니다: " + gameId));

        UUID playerId = req.playerId();

        // 1. 권한 체크
        if (game.getCurrentTurnSeatNo() == null) {
            return CommitTurnResponse.fail(gameId, "현재 턴 정보 없음");
        }
        GamePlayerState ps = playerStateRepository.findByGameIdAndPlayerId(gameId, playerId)
                .orElseThrow(() -> new IllegalStateException("플레이어 상태 없음"));
        if (!ps.getSeatNo().equals(game.getCurrentTurnSeatNo())) {
            return CommitTurnResponse.fail(gameId, "당신의 턴이 아닙니다");
        }

        log.info("[COMMIT_TURN] 진입: game={}, player={}, newBuildings={}, upgrades={}, techTiles={}, leechTargets={}",
                gameId, playerId,
                safeSize(req.newBuildings()),
                safeSize(req.upgradedBuildings()),
                safeSize(req.newTechTiles()),
                safeSize(req.leechTargets()));

        // 1b. 경량 불변식 검증 (Phase 0a) — FE 버그가 DB 로 새는 것을 차단
        String violation = validateInvariants(ps, req);
        if (violation != null) {
            log.warn("[COMMIT_TURN] 검증 실패: game={}, player={}, reason={}", gameId, playerId, violation);
            return CommitTurnResponse.fail(gameId, violation);
        }

        // 2. PlayerState 스냅샷 덮어쓰기 전에 4→5 전환 감지용 이전 트랙 레벨 스냅
        int oldTerra = ps.getTechTerraforming();
        int oldNav   = ps.getTechNavigation();
        int oldAi    = ps.getTechAi();
        int oldGaia  = ps.getTechGaia();
        int oldEco   = ps.getTechEconomy();
        int oldSci   = ps.getTechScience();

        if (req.playerState() != null) {
            ps.applySnapshot(req.playerState());
            playerStateRepository.save(ps);
        }

        // 2b. 트랙 4 → 5 진입 감지 → BE 측 side effect (토큰 플립 + TERRA 위 연방 토큰 획득)
        if (oldTerra == 4 && ps.getTechTerraforming() == 5) techTileService.handleTrackLevel5Entry(gameId, playerId, "TERRA_FORMING");
        if (oldNav   == 4 && ps.getTechNavigation() == 5)   techTileService.handleTrackLevel5Entry(gameId, playerId, "NAVIGATION");
        if (oldAi    == 4 && ps.getTechAi() == 5)           techTileService.handleTrackLevel5Entry(gameId, playerId, "AI");
        if (oldGaia  == 4 && ps.getTechGaia() == 5)         techTileService.handleTrackLevel5Entry(gameId, playerId, "GAIA_FORMING");
        if (oldEco   == 4 && ps.getTechEconomy() == 5)      techTileService.handleTrackLevel5Entry(gameId, playerId, "ECONOMY");
        if (oldSci   == 4 && ps.getTechScience() == 5)      techTileService.handleTrackLevel5Entry(gameId, playerId, "SCIENCE");

        // 3. 헥스 변경 (테라포밍 / 블랙행성)
        if (req.hexChanges() != null) {
            for (var hc : req.hexChanges()) {
                GameHex hex = hexRepository.findByGameIdAndHexQAndHexR(gameId, hc.hexQ(), hc.hexR()).orElse(null);
                if (hex == null) {
                    log.warn("[COMMIT_TURN] 헥스 없음: ({},{})", hc.hexQ(), hc.hexR());
                    continue;
                }
                try {
                    hex.setPlanetType(PlanetType.valueOf(hc.newPlanetType()));
                    hexRepository.save(hex);
                } catch (IllegalArgumentException e) {
                    log.warn("[COMMIT_TURN] 알 수 없는 planet_type: {}", hc.newPlanetType());
                }
            }
        }

        // 4. 신규 건물 INSERT
        if (req.newBuildings() != null) {
            for (var nb : req.newBuildings()) {
                BuildingType btype;
                try {
                    btype = BuildingType.valueOf(nb.buildingType());
                } catch (IllegalArgumentException e) {
                    log.warn("[COMMIT_TURN] 알 수 없는 building_type: {}", nb.buildingType());
                    continue;
                }
                GameBuilding b = GameBuilding.place(gameId, playerId, nb.hexQ(), nb.hexR(), btype);
                if (Boolean.TRUE.equals(nb.isLantidsMine())) {
                    b.markAsLantidsMine();
                }
                buildingRepository.save(b);
                // 연방 자동 편입: 인접한 기존 연방 그룹이 있으면 자동으로 포함
                // (파워 0 건물과 란티다 기생은 제외)
                if (btype != BuildingType.GAIAFORMER && !Boolean.TRUE.equals(nb.isLantidsMine())) {
                    federationFormService.autoJoinFederation(gameId, playerId, nb.hexQ(), nb.hexR());
                }
            }
        }

        // 5. 업그레이드 건물 UPDATE
        if (req.upgradedBuildings() != null) {
            for (var ub : req.upgradedBuildings()) {
                GameBuilding b = buildingRepository
                        .findFirstByGameIdAndHexQAndHexRAndIsLantidsMine(gameId, ub.hexQ(), ub.hexR(), false)
                        .orElse(null);
                if (b == null) {
                    log.warn("[COMMIT_TURN] 업그레이드 대상 건물 없음: ({},{})", ub.hexQ(), ub.hexR());
                    continue;
                }
                BuildingType newType;
                try {
                    newType = BuildingType.valueOf(ub.newBuildingType());
                } catch (IllegalArgumentException e) {
                    log.warn("[COMMIT_TURN] 알 수 없는 building_type: {}", ub.newBuildingType());
                    continue;
                }
                // ACADEMY 업그레이드는 academyType(KNOWLEDGE/QIC) 설정 필요 — 누락 시 건물 수입 0
                if (newType == BuildingType.ACADEMY && ub.academyType() != null && !ub.academyType().isBlank()) {
                    try {
                        var atype = com.gaiaproject.domain.enumtype.building.AcademyType.valueOf(ub.academyType());
                        b.upgradeToAcademy(atype);
                    } catch (IllegalArgumentException e) {
                        log.warn("[COMMIT_TURN] 알 수 없는 academy_type: {} — 일반 upgrade 로 폴백", ub.academyType());
                        b.upgrade(newType);
                    }
                } else {
                    b.upgrade(newType);
                }
                buildingRepository.save(b);

                // 글린 PI 건설 시 전용 연방 토큰 자동 지급
                if (newType == BuildingType.PLANETARY_INSTITUTE && ps.getFactionType() == FactionType.GLEENS) {
                    boolean alreadyHas = playerFederationTokenRepository.findByGameIdAndPlayerId(gameId, playerId).stream()
                            .anyMatch(t -> t.getFederationTileType() == FederationTileType.GLEENS_FEDERATION);
                    if (!alreadyHas) {
                        playerFederationTokenRepository.save(GamePlayerFederationToken.builder()
                                .gameId(gameId).playerId(playerId)
                                .federationTileType(FederationTileType.GLEENS_FEDERATION)
                                .build());
                        log.info("[COMMIT_TURN] 글린 PI 전용 연방 토큰 지급: player={}", playerId);
                    }
                }
            }
        }

        // 6. 기술타일 INSERT + 고급타일 offer taken 처리
        if (req.newTechTiles() != null) {
            for (var nt : req.newTechTiles()) {
                if (techTileRepository.existsByGameIdAndPlayerIdAndTechTileCode(gameId, playerId, nt.tileCode())) {
                    continue; // 중복 방지
                }
                GamePlayerTechTile t = GamePlayerTechTile.builder()
                        .gameId(gameId)
                        .playerId(playerId)
                        .techTileCode(nt.tileCode())
                        .build();
                techTileRepository.save(t);

                // 고급 타일은 offer에도 taken 마킹
                if (nt.tileCode().startsWith("ADV_")) {
                    try {
                        var advCode = com.gaiaproject.domain.enumtype.tech.AdvancedTechTileCode.valueOf(nt.tileCode());
                        advTechOfferRepository.findByGameIdAndAdvTechTileCode(gameId, advCode).ifPresent(offer -> {
                            offer.take(playerId);
                            advTechOfferRepository.save(offer);
                        });
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }

        // 6b. 이번 턴에 사용한 ACTION 타입 기술타일 → action_used = true 마킹 (라운드 1회 제한)
        if (req.usedTechTileActions() != null) {
            for (String tileCode : req.usedTechTileActions()) {
                if (tileCode == null || tileCode.isBlank()) continue;
                techTileRepository.findByGameIdAndPlayerIdAndTechTileCode(gameId, playerId, tileCode)
                        .ifPresent(t -> {
                            if (!t.isActionUsed()) {
                                t.useAction();
                                techTileRepository.save(t);
                                log.info("[COMMIT_TURN] 기술타일 ACTION 사용 마킹: player={}, tile={}", playerId, tileCode);
                            }
                        });
            }
        }

        // 6c. 이번 턴에 사용한 공용 파워 액션(PWR_*) → GamePowerActionUsage INSERT (라운드 1회 제한)
        // 함대 파워 액션(FLEET_*)은 GameAction 테이블에서 파싱하므로 여기서 처리 안 함 (PowerActionService.getUsedPowerActionCodes 참고)
        if (req.usedCommonPowerActions() != null) {
            int round = game.getCurrentRound() != null ? game.getCurrentRound() : 1;
            for (String actionCode : req.usedCommonPowerActions()) {
                if (actionCode == null || actionCode.isBlank()) continue;
                PowerActionType usageType;
                try {
                    usageType = PowerActionType.valueOf(actionCode);
                } catch (IllegalArgumentException e) {
                    log.warn("[COMMIT_TURN] 알 수 없는 power action code: {}", actionCode);
                    continue;
                }
                // 중복 방지
                if (powerActionUsageRepository.findByGameIdAndRoundNumberAndPowerActionType(gameId, round, usageType).isPresent()) {
                    continue;
                }
                powerActionUsageRepository.save(GamePowerActionUsage.builder()
                        .gameId(gameId)
                        .roundNumber(round)
                        .powerActionType(usageType)
                        .playerId(playerId)
                        .build());
                log.info("[COMMIT_TURN] 파워 액션 사용 기록: player={}, round={}, action={}", playerId, round, usageType);
            }
        }

        // 7. 고급타일로 덮인 기본타일 UPDATE
        if (req.coveredTiles() != null) {
            for (var ct : req.coveredTiles()) {
                GamePlayerTechTile t = techTileRepository
                        .findByGameIdAndPlayerIdAndTechTileCode(gameId, playerId, ct.tileCode())
                        .orElse(null);
                if (t != null) {
                    t.cover(ct.coveredByCode());
                    techTileRepository.save(t);
                }
            }
        }

        // 8. 연방 그룹 INSERT
        if (req.newFederationGroups() != null) {
            for (var fg : req.newFederationGroups()) {
                federationFormService.createGroupRaw(gameId, playerId, fg.tileCode(),
                        fg.buildingHexes(), fg.tokenHexes());
            }
        }

        // 8b. 연방 토큰 플립 (used 처리)
        if (req.flippedFederationTokens() != null) {
            for (String tileCode : req.flippedFederationTokens()) {
                federationFormService.flipTokenRaw(gameId, playerId, tileCode);
            }
        }

        // 8c. 인공물 획득 — 플레이어 인공물 row INSERT + 오퍼(GameArtifactOffer) acquire 마킹
        // 오퍼 마킹 누락 시 UI 에서는 "미획득" 으로 계속 보이고 다른 플레이어가 동일 인공물을 가져갈 수 있음 (BUG_REPORTS #11)
        if (req.newArtifacts() != null) {
            for (String artifactType : req.newArtifacts()) {
                if (!playerArtifactRepository.existsByGameIdAndPlayerIdAndArtifactType(gameId, playerId, artifactType)) {
                    playerArtifactRepository.save(GamePlayerArtifact.builder()
                            .gameId(gameId).playerId(playerId).artifactType(artifactType).build());
                }
                // 오퍼 획득 마킹 (이미 획득되어 있으면 skip)
                try {
                    var artType = com.gaiaproject.domain.enumtype.artifact.ArtifactType.valueOf(artifactType);
                    artifactOfferRepository.findByGameIdAndArtifactType(gameId, artType)
                            .ifPresent(offer -> {
                                if (!Boolean.TRUE.equals(offer.getIsAcquired())) {
                                    offer.acquire(playerId);
                                    artifactOfferRepository.save(offer);
                                    log.info("[COMMIT_TURN] 인공물 오퍼 획득: player={}, artifact={}", playerId, artifactType);
                                }
                            });
                } catch (IllegalArgumentException e) {
                    log.warn("[COMMIT_TURN] 알 수 없는 artifact type: {}", artifactType);
                }
            }
        }

        // 8d. 건물 링 추가 (MOWEIDS_RING)
        if (req.ringedBuildings() != null) {
            for (var rc : req.ringedBuildings()) {
                GameBuilding b = buildingRepository
                        .findFirstByGameIdAndHexQAndHexRAndIsLantidsMine(gameId, rc.hexQ(), rc.hexR(), false)
                        .orElse(null);
                if (b != null) {
                    b.applyRing();
                    buildingRepository.save(b);
                }
            }
        }

        // 8e. 건물 다운그레이드 (FIRAKS_DOWNGRADE: RL → TS)
        if (req.downgradedToTs() != null) {
            for (var dc : req.downgradedToTs()) {
                GameBuilding b = buildingRepository
                        .findFirstByGameIdAndHexQAndHexRAndIsLantidsMine(gameId, dc.hexQ(), dc.hexR(), false)
                        .orElse(null);
                if (b != null) {
                    b.upgrade(BuildingType.TRADING_STATION);
                    buildingRepository.save(b);
                }
            }
        }

        // 8e-2. 가이아포머 반환 (GAIAFORMER → MINE upgrade + 연방 자동 편입)
        //   FE 가 stockGaiaformer +1 은 snapshot 으로 반영하고, BE 는 건물 타입 변환과 연방 편입만 처리 (BUG_REPORTS #16)
        if (req.gaiaformerReturns() != null) {
            for (var gc : req.gaiaformerReturns()) {
                GameBuilding b = buildingRepository
                        .findFirstByGameIdAndHexQAndHexRAndIsLantidsMine(gameId, gc.hexQ(), gc.hexR(), false)
                        .orElse(null);
                if (b == null) {
                    log.warn("[COMMIT_TURN] 가이아포머 반환 대상 없음: ({},{})", gc.hexQ(), gc.hexR());
                    continue;
                }
                if (b.getBuildingType() != BuildingType.GAIAFORMER || !b.getPlayerId().equals(playerId)) {
                    log.warn("[COMMIT_TURN] 가이아포머 반환 부적합 (type={}, owner={}): ({},{})",
                            b.getBuildingType(), b.getPlayerId(), gc.hexQ(), gc.hexR());
                    continue;
                }
                b.upgrade(BuildingType.MINE);
                buildingRepository.save(b);
                federationFormService.autoJoinFederation(gameId, playerId, gc.hexQ(), gc.hexR());
                log.info("[COMMIT_TURN] 가이아포머 반환: player={}, hex=({},{})", playerId, gc.hexQ(), gc.hexR());
            }
        }

        // 8f. 엠바스 교환 (PI ↔ Mine 좌표 스왑)
        if (req.ambasSwap() != null) {
            var swap = req.ambasSwap();
            GameBuilding pi = buildingRepository
                    .findFirstByGameIdAndHexQAndHexRAndIsLantidsMine(gameId, swap.piHexQ(), swap.piHexR(), false)
                    .orElse(null);
            GameBuilding mine = buildingRepository
                    .findFirstByGameIdAndHexQAndHexRAndIsLantidsMine(gameId, swap.mineHexQ(), swap.mineHexR(), false)
                    .orElse(null);
            if (pi != null && mine != null) {
                // 타입 스왑
                pi.upgrade(BuildingType.MINE);
                mine.upgrade(BuildingType.PLANETARY_INSTITUTE);
                buildingRepository.save(pi);
                buildingRepository.save(mine);
            }
        }

        // 8g. 함대 입장 (FLEET_PROBE)
        if (req.fleetProbeName() != null && !req.fleetProbeName().isBlank()) {
            if (!fleetProbeRepository.existsByGameIdAndPlayerIdAndFleetName(gameId, playerId, req.fleetProbeName())) {
                fleetProbeRepository.save(GamePlayerFleetProbe.builder()
                        .gameId(gameId).playerId(playerId).fleetName(req.fleetProbeName()).build());
            }
        }

        // 9. 액션 로그 INSERT
        if (req.actionLog() != null) {
            for (var entry : req.actionLog()) {
                try {
                    actionService.saveActionOnly(gameId, playerId,
                            ActionType.valueOf(entry.actionType()), entry.actionData());
                } catch (Exception e) {
                    log.warn("[COMMIT_TURN] action log 저장 실패: {}", e.getMessage());
                }
            }
        }

        // 10. VP 로그 INSERT
        if (req.vpLog() != null) {
            for (var entry : req.vpLog()) {
                try {
                    vpLogService.logVp(gameId, playerId,
                            VpCategory.valueOf(entry.category()), entry.amount(),
                            game.getCurrentRound(), entry.description());
                } catch (Exception e) {
                    log.warn("[COMMIT_TURN] vp log 저장 실패: {}", e.getMessage());
                }
            }
        }

        // 11. 리치 + 턴 진행
        //   - FE는 leechTargets를 직접 계산하지 않음. BE의 PowerLeechService가 배치된 건물 기준으로 계산.
        //   - 배치된 건물을 순회하며 createBatchAndProcess 호출 → 내부적으로 리치 batch 생성 + WS 브로드캐스트 + 턴 진행 담당.
        //   - 복수 건물일 경우 MINE_LEECH / LOST_PLANET_LEECH follow-up 체인 활용.

        List<PlacedBuilding> placed = collectPlacedBuildings(req);
        boolean hasLeech = !placed.isEmpty() && anyBuildingGeneratesLeech(gameId, playerId, placed);
        String leechBatchKey = null;

        if (placed.isEmpty()) {
            // 건물 배치 없음 → 즉시 턴 진행
            actionService.advanceTurnAndBroadcast(game);
        } else {
            // 첫 번째 건물로 리치 배치 생성.
            // 두 번째 이후는 follow-up chain으로 연결.
            List<GameBuilding> allBuildings = buildingRepository.findByGameId(gameId);
            PlacedBuilding first = placed.get(0);
            String followUpType = null;
            String followUpData = null;

            if (placed.size() >= 2) {
                PlacedBuilding second = placed.get(1);
                followUpType = leechFollowUpType(second.buildingType);
                if (placed.size() >= 3) {
                    PlacedBuilding third = placed.get(2);
                    String nextFollow = leechFollowUpType(third.buildingType);
                    followUpData = String.format(
                            "{\"hexQ\":%d,\"hexR\":%d,\"playerId\":\"%s\",\"nextFollowUp\":\"%s\",\"lpHexQ\":%d,\"lpHexR\":%d}",
                            second.hexQ, second.hexR, playerId, nextFollow, third.hexQ, third.hexR);
                } else {
                    followUpData = String.format(
                            "{\"hexQ\":%d,\"hexR\":%d,\"playerId\":\"%s\"}",
                            second.hexQ, second.hexR, playerId);
                }
            }

            powerLeechService.createBatchAndProcess(game, playerId,
                    first.hexQ, first.hexR, first.buildingType,
                    allBuildings, followUpType, followUpData);
        }

        // 12. 전체 상태 갱신 브로드캐스트
        webSocketService.broadcastStateUpdated(gameId);

        Integer nextSeatNo = game.getCurrentTurnSeatNo();
        log.info("[COMMIT_TURN] 완료: game={}, player={}, nextSeat={}, placed={}",
                gameId, playerId, nextSeatNo, placed.size());

        // 확정 직후의 최신 스냅샷을 응답에 포함 (WS STATE_UPDATED와 동일 DTO).
        // FE는 이 스냅샷으로 usedPowerActionCodes/techTileData 등을 즉시 동기화 → WS 도착 지연에 따른 race 제거.
        GameSnapshot snapshot = gameSnapshotService.buildSnapshot(gameId);

        return CommitTurnResponse.success(gameId, nextSeatNo, hasLeech, leechBatchKey, snapshot);
    }

    private static int safeSize(List<?> list) {
        return list == null ? 0 : list.size();
    }

    /** 배치된 건물 (신규 + 업그레이드) */
    private record PlacedBuilding(int hexQ, int hexR, BuildingType buildingType) {}

    /** commit 요청에서 배치된 모든 건물을 순서대로 수집 */
    private List<PlacedBuilding> collectPlacedBuildings(CommitTurnRequest req) {
        List<PlacedBuilding> out = new ArrayList<>();
        if (req.newBuildings() != null) {
            for (var nb : req.newBuildings()) {
                try {
                    out.add(new PlacedBuilding(nb.hexQ(), nb.hexR(), BuildingType.valueOf(nb.buildingType())));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        if (req.upgradedBuildings() != null) {
            for (var ub : req.upgradedBuildings()) {
                try {
                    out.add(new PlacedBuilding(ub.hexQ(), ub.hexR(), BuildingType.valueOf(ub.newBuildingType())));
                } catch (IllegalArgumentException ignored) {}
            }
        }
        // FIRAKS_DOWNGRADE (RL → TS) 도 리치 트리거 대상 — 레거시 FactionAbilityService.handleFiraksPiDowngrade 동일 동작 (BUG_REPORTS #14)
        if (req.downgradedToTs() != null) {
            for (var dc : req.downgradedToTs()) {
                out.add(new PlacedBuilding(dc.hexQ(), dc.hexR(), BuildingType.TRADING_STATION));
            }
        }
        // 가이아포머 반환 (GAIAFORMER → MINE) 도 리치 트리거 대상 — 레거시 placeMineInPlay 동일 동작 (BUG_REPORTS #16)
        if (req.gaiaformerReturns() != null) {
            for (var gc : req.gaiaformerReturns()) {
                out.add(new PlacedBuilding(gc.hexQ(), gc.hexR(), BuildingType.MINE));
            }
        }
        return out;
    }

    /** 건물 배치로 인해 리치가 발생할 수 있는지 (가이아포머 등 파워 0 건물은 제외) */
    private boolean anyBuildingGeneratesLeech(UUID gameId, UUID playerId, List<PlacedBuilding> placed) {
        for (var p : placed) {
            if (p.buildingType != BuildingType.GAIAFORMER) return true;
        }
        return false;
    }

    /** 건물 타입에 맞는 follow-up leech 타입 */
    private String leechFollowUpType(BuildingType type) {
        if (type == BuildingType.LOST_PLANET_MINE) return "LOST_PLANET_LEECH";
        return "MINE_LEECH";
    }

    // ─────────────────────────────────────────────────────────────
    // Phase 0a — 경량 불변식 검증
    //
    // 목적: FE 의 `calculatePreviewState` 버그가 DB 에 오염된 값을 남기는 것을 차단.
    // 부정행위 방지(=풀 재시뮬레이션)는 범위 밖 (Phase 0b 에서 검토).
    //
    // 정책
    //   HARD FAIL: 자원 음수/상한 초과, 파워 bowl 음수, 기술트랙 범위 이탈/역행, 건물 재고 범위 이탈
    //   WARN ONLY: VP 감소 (리치 시점에 발생 가능), actionLog 부재
    // ─────────────────────────────────────────────────────────────
    /**
     * @return 검증 실패 사유 (null 이면 통과)
     */
    private String validateInvariants(GamePlayerState before, CommitTurnRequest req) {
        PlayerStateSnapshot s = req.playerState();
        if (s == null) {
            // 스냅샷이 없는 commit (가능한지 불명확, 일단 통과)
            return null;
        }

        // 1. 자원 음수 방어
        if (nz(s.credit()) < 0 || nz(s.ore()) < 0 || nz(s.knowledge()) < 0 || nz(s.qic()) < 0) {
            return String.format("자원 음수: credit=%s, ore=%s, knowledge=%s, qic=%s",
                    s.credit(), s.ore(), s.knowledge(), s.qic());
        }

        // 2. 자원 상한 초과 방어 (qic 는 무제한)
        if (nz(s.credit()) > GamePlayerState.MAX_CREDIT)        return "credit 상한 초과: " + s.credit();
        if (nz(s.ore()) > GamePlayerState.MAX_ORE)              return "ore 상한 초과: " + s.ore();
        if (nz(s.knowledge()) > GamePlayerState.MAX_KNOWLEDGE)  return "knowledge 상한 초과: " + s.knowledge();

        // 3. 파워 Bowl / 가이아 파워 음수
        if (nz(s.powerBowl1()) < 0 || nz(s.powerBowl2()) < 0 || nz(s.powerBowl3()) < 0 || nz(s.gaiaPower()) < 0) {
            return String.format("파워 값 음수: bowl1=%s, bowl2=%s, bowl3=%s, gaia=%s",
                    s.powerBowl1(), s.powerBowl2(), s.powerBowl3(), s.gaiaPower());
        }

        // 4. 브레인스톤 값 검증 (null 또는 0/1/2/3)
        //    0 = 가이아 구역 (타클론 함대 입장 시 브레인스톤이 가이아로 이동; 라운드 시작 시 bowl1 로 복귀)
        if (s.brainstoneBowl() != null) {
            int b = s.brainstoneBowl();
            if (b != 0 && b != 1 && b != 2 && b != 3) {
                return "brainstoneBowl 값 비정상: " + b;
            }
        }

        // 5. 기술 트랙 범위 (0 ~ 5)
        String trackErr = validateTrackRange(s);
        if (trackErr != null) return trackErr;

        // 6. 기술 트랙 역행 금지 (단조 증가)
        String regressErr = validateTrackMonotonic(before, s);
        if (regressErr != null) return regressErr;

        // 7. 건물 재고 범위
        String stockErr = validateStockRange(s);
        if (stockErr != null) return stockErr;

        // 8. VP 감소 (Warn only — 리치 결정 경로에서 VP 가 감소할 가능성 대비)
        if (s.victoryPoints() != null && s.victoryPoints() < before.getVictoryPoints()) {
            log.warn("[COMMIT_TURN][VP_DROP] player={} vp: {} -> {}",
                    before.getPlayerId(), before.getVictoryPoints(), s.victoryPoints());
        }

        // 9. actionLog 부재 (Warn only — audit 용이므로 강제 거절하지 않음)
        if (req.actionLog() == null || req.actionLog().isEmpty()) {
            log.warn("[COMMIT_TURN] actionLog 없음 — audit 추적 불가");
        }

        return null; // 통과
    }

    /** null Integer → 0 */
    private static int nz(Integer v) {
        return v == null ? 0 : v;
    }

    private String validateTrackRange(PlayerStateSnapshot s) {
        int max = GamePlayerState.MAX_TECH_TRACK_LEVEL;
        if (s.techTerraforming() != null && (s.techTerraforming() < 0 || s.techTerraforming() > max)) return "techTerraforming 범위 이탈: " + s.techTerraforming();
        if (s.techNavigation() != null   && (s.techNavigation() < 0   || s.techNavigation() > max))   return "techNavigation 범위 이탈: " + s.techNavigation();
        if (s.techAi() != null           && (s.techAi() < 0           || s.techAi() > max))           return "techAi 범위 이탈: " + s.techAi();
        if (s.techGaia() != null         && (s.techGaia() < 0         || s.techGaia() > max))         return "techGaia 범위 이탈: " + s.techGaia();
        if (s.techEconomy() != null      && (s.techEconomy() < 0      || s.techEconomy() > max))      return "techEconomy 범위 이탈: " + s.techEconomy();
        if (s.techScience() != null      && (s.techScience() < 0      || s.techScience() > max))      return "techScience 범위 이탈: " + s.techScience();
        return null;
    }

    private String validateTrackMonotonic(GamePlayerState before, PlayerStateSnapshot s) {
        if (s.techTerraforming() != null && s.techTerraforming() < before.getTechTerraforming()) return "techTerraforming 역행: " + before.getTechTerraforming() + " -> " + s.techTerraforming();
        if (s.techNavigation() != null   && s.techNavigation() < before.getTechNavigation())     return "techNavigation 역행: " + before.getTechNavigation() + " -> " + s.techNavigation();
        if (s.techAi() != null           && s.techAi() < before.getTechAi())                     return "techAi 역행: " + before.getTechAi() + " -> " + s.techAi();
        if (s.techGaia() != null         && s.techGaia() < before.getTechGaia())                 return "techGaia 역행: " + before.getTechGaia() + " -> " + s.techGaia();
        if (s.techEconomy() != null      && s.techEconomy() < before.getTechEconomy())           return "techEconomy 역행: " + before.getTechEconomy() + " -> " + s.techEconomy();
        if (s.techScience() != null      && s.techScience() < before.getTechScience())           return "techScience 역행: " + before.getTechScience() + " -> " + s.techScience();
        return null;
    }

    private String validateStockRange(PlayerStateSnapshot s) {
        if (s.stockMine() != null                && (s.stockMine() < 0                || s.stockMine() > GamePlayerState.MAX_STOCK_MINE))                                return "stockMine 범위 이탈: " + s.stockMine();
        if (s.stockTradingStation() != null      && (s.stockTradingStation() < 0      || s.stockTradingStation() > GamePlayerState.MAX_STOCK_TRADING_STATION))          return "stockTradingStation 범위 이탈: " + s.stockTradingStation();
        if (s.stockResearchLab() != null         && (s.stockResearchLab() < 0         || s.stockResearchLab() > GamePlayerState.MAX_STOCK_RESEARCH_LAB))                return "stockResearchLab 범위 이탈: " + s.stockResearchLab();
        if (s.stockPlanetaryInstitute() != null  && (s.stockPlanetaryInstitute() < 0  || s.stockPlanetaryInstitute() > GamePlayerState.MAX_STOCK_PLANETARY_INSTITUTE))  return "stockPlanetaryInstitute 범위 이탈: " + s.stockPlanetaryInstitute();
        if (s.stockAcademy() != null             && (s.stockAcademy() < 0             || s.stockAcademy() > GamePlayerState.MAX_STOCK_ACADEMY))                         return "stockAcademy 범위 이탈: " + s.stockAcademy();
        if (s.stockGaiaformer() != null          && (s.stockGaiaformer() < 0          || s.stockGaiaformer() > GamePlayerState.MAX_STOCK_GAIAFORMER))                   return "stockGaiaformer 범위 이탈: " + s.stockGaiaformer();
        return null;
    }
}
