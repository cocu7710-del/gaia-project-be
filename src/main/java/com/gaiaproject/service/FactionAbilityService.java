package com.gaiaproject.service;

import com.gaiaproject.domain.entity.building.GameBuilding;
import com.gaiaproject.domain.entity.player.GamePlayerFederationToken;
import com.gaiaproject.domain.entity.player.GamePlayerState;
import com.gaiaproject.domain.enumtype.action.ActionType;
import com.gaiaproject.domain.enumtype.building.BuildingType;
import com.gaiaproject.domain.enumtype.federation.FederationTileType;
import com.gaiaproject.domain.enumtype.player.FactionType;
import com.gaiaproject.dto.request.FactionAbilityRequest;
import com.gaiaproject.dto.response.FactionAbilityResponse;
import com.gaiaproject.repository.building.GameBuildingRepository;
import com.gaiaproject.repository.player.GamePlayerFederationTokenRepository;
import com.gaiaproject.repository.player.GamePlayerStateRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 종족 고유 능력 처리 서비스
 *
 * 기본 능력:
 * - BAL_TAKS_CONVERT_GAIAFORMER : (프리 액션) 가이아포머 → QIC
 * - XENOS_ORE_TO_POWER          : (프리 액션) 광석 1 → 3구역 파워 1
 * - BESCODS_ADVANCE_LOWEST_TRACK: (액션) 최저 기술 트랙 +1
 * - SPACE_GIANTS_TERRAFORM_2    : (액션) 2삽 테라포밍 선언
 * - GLEENS_JUMP                 : (액션) 2거리 점프 선언
 *
 * PI 능력:
 * - FIRAKS_DOWNGRADE            : (액션, 라운드당 1회) 연구소→교역소 + 지식트랙 1칸
 * - AMBAS_SWAP                  : (액션, 라운드당 1회) 광산↔의회 위치 교환
 * - HADSCH_HALLAS_CREDIT_CONVERT: (프리 액션) 크레딧으로 자원 변환 (trackCode=ORE/KNOWLEDGE/QIC)
 * - GLEENS_FEDERATION_TOKEN     : (액션) 2크레딧+1광석+1지식 → 연방 토큰 획득
 * - ITARS_GAIA_TO_TECH_TILE     : (액션) 4 가이아파워 영구 제거 → 기본 기술타일 (FE에서 타일 선택)
 * - IVITS_PLACE_STATION         : TODO
 * - MOWEIDS_RING                : TODO
 * - TINKEROIDS_ACTION           : TODO
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FactionAbilityService {

    private final GamePlayerStateRepository playerStateRepository;
    private final GameBuildingRepository buildingRepository;
    private final GamePlayerFederationTokenRepository federationTokenRepository;
    private final ActionService actionService;

    /** 의회 건설 여부 확인 */
    private boolean hasPi(GamePlayerState ps) {
        return ps.getStockPlanetaryInstitute() == 0;
    }

    public FactionAbilityResponse useAbility(UUID gameId, FactionAbilityRequest request) {
        String code = request.abilityCode();
        UUID playerId = request.playerId();

        GamePlayerState ps = playerStateRepository.findByGameIdAndPlayerId(gameId, playerId)
                .orElseThrow(() -> new IllegalStateException("플레이어 상태를 찾을 수 없습니다"));

        try {
            return switch (code) {
                // ── 기본 능력 ──
                case "BAL_TAKS_CONVERT_GAIAFORMER"  -> handleBaltaksConvert(gameId, playerId, ps, code);
                case "XENOS_ORE_TO_POWER"           -> handleXenosOreToPower(gameId, playerId, ps, code);
                case "BESCODS_ADVANCE_LOWEST_TRACK" -> handleBescodsAdvance(gameId, playerId, ps, code);
                case "SPACE_GIANTS_TERRAFORM_2"     -> handleSpaceGiantsTerraform(gameId, playerId, ps, code);
                case "GLEENS_JUMP"                  -> handleGleensJump(gameId, playerId, ps, code);
                // ── PI 능력 ──
                case "FIRAKS_DOWNGRADE"             -> handleFiraksPiDowngrade(gameId, playerId, ps, code, request);
                case "AMBAS_SWAP"                   -> handleAmbasPiSwap(gameId, playerId, ps, code, request);
                case "HADSCH_HALLAS_CREDIT_CONVERT" -> handleHadschHallasCreditConvert(gameId, playerId, ps, code, request);
                case "GLEENS_FEDERATION_TOKEN"      -> handleGleensFederationToken(gameId, playerId, ps, code);
                case "ITARS_GAIA_TO_TECH_TILE"      -> handleItarsGaiaToTechTile(gameId, playerId, ps, code);
                default -> FactionAbilityResponse.fail(gameId, code, "알 수 없는 능력 코드: " + code);
            };
        } catch (IllegalStateException e) {
            return FactionAbilityResponse.fail(gameId, code, e.getMessage());
        }
    }

    // ═══════════════════════════════════════════════════════════
    // 기본 능력
    // ═══════════════════════════════════════════════════════════

    /** 발타크: 가이아포머 → QIC (프리 액션) */
    private FactionAbilityResponse handleBaltaksConvert(UUID gameId, UUID playerId, GamePlayerState ps, String code) {
        if (ps.getFactionType() != FactionType.BAL_TAKS) return FactionAbilityResponse.fail(gameId, code, "발타크만 사용 가능");
        ps.convertGaiaformerToQic();
        playerStateRepository.save(ps);
        log.info("[BAL_TAKS] 포머→QIC: player={}", playerId);
        return FactionAbilityResponse.success(gameId, code, null);
    }

    /** 제노스: 광석 1 → 3구역 파워 1 (프리 액션) */
    private FactionAbilityResponse handleXenosOreToPower(UUID gameId, UUID playerId, GamePlayerState ps, String code) {
        if (ps.getFactionType() != FactionType.XENOS) return FactionAbilityResponse.fail(gameId, code, "제노스만 사용 가능");
        ps.spendOre(1);
        ps.gainPower(1);
        playerStateRepository.save(ps);
        log.info("[XENOS] 광석→파워3: player={}", playerId);
        return FactionAbilityResponse.success(gameId, code, null);
    }

    /** 매드안드로이드: 최저 기술 트랙 +1 (액션) */
    private FactionAbilityResponse handleBescodsAdvance(UUID gameId, UUID playerId, GamePlayerState ps, String code) {
        if (ps.getFactionType() != FactionType.BESCODS) return FactionAbilityResponse.fail(gameId, code, "매드안드로이드만 사용 가능");
        if (ps.isFactionAbilityUsed()) return FactionAbilityResponse.fail(gameId, code, "이번 라운드 이미 사용했습니다");
        String track = ps.advanceLowestTechTrack();
        ps.markFactionAbilityUsed();
        playerStateRepository.save(ps);
        log.info("[BESCODS] 최저트랙 전진: player={}, track={}", playerId, track);
        var result = actionService.saveActionAndNextTurn(gameId, playerId, ActionType.FACTION_ABILITY,
                "{\"abilityCode\":\"" + code + "\",\"track\":\"" + track + "\"}");
        return FactionAbilityResponse.success(gameId, code, result.nextTurnSeatNo());
    }

    /** 스페이스 자이언트: 2삽 선언 (액션, FE에서 광산 건설로 연결) */
    private FactionAbilityResponse handleSpaceGiantsTerraform(UUID gameId, UUID playerId, GamePlayerState ps, String code) {
        if (ps.getFactionType() != FactionType.SPACE_GIANTS) return FactionAbilityResponse.fail(gameId, code, "스페이스 자이언트만 사용 가능");
        if (ps.isFactionAbilityUsed()) return FactionAbilityResponse.fail(gameId, code, "이번 라운드 이미 사용했습니다");
        ps.markFactionAbilityUsed();
        playerStateRepository.save(ps);
        return FactionAbilityResponse.success(gameId, code, null);
    }

    /** 글린: 2거리 점프 선언 (액션) */
    private FactionAbilityResponse handleGleensJump(UUID gameId, UUID playerId, GamePlayerState ps, String code) {
        if (ps.getFactionType() != FactionType.GLEENS) return FactionAbilityResponse.fail(gameId, code, "글린만 사용 가능");
        if (ps.isFactionAbilityUsed()) return FactionAbilityResponse.fail(gameId, code, "이번 라운드 이미 사용했습니다");
        ps.markFactionAbilityUsed();
        playerStateRepository.save(ps);
        return FactionAbilityResponse.success(gameId, code, null);
    }

    // ═══════════════════════════════════════════════════════════
    // PI 능력
    // ═══════════════════════════════════════════════════════════

    /**
     * 파이락 PI: 연구소 → 교역소 다운그레이드 + 지식 트랙 1칸 (라운드당 1회)
     * request.hexQ/hexR = 다운그레이드할 연구소 위치
     * request.trackCode = 전진할 기술 트랙 코드
     */
    private FactionAbilityResponse handleFiraksPiDowngrade(UUID gameId, UUID playerId, GamePlayerState ps, String code, FactionAbilityRequest req) {
        if (ps.getFactionType() != FactionType.FIRAKS) return FactionAbilityResponse.fail(gameId, code, "파이락만 사용 가능");
        if (!hasPi(ps)) return FactionAbilityResponse.fail(gameId, code, "행성 의회 건설 후 사용 가능합니다");
        if (ps.isFactionAbilityUsed()) return FactionAbilityResponse.fail(gameId, code, "이번 라운드 이미 사용했습니다");
        if (req.hexQ() == null || req.hexR() == null) return FactionAbilityResponse.fail(gameId, code, "연구소 위치를 지정해야 합니다");
        if (req.trackCode() == null) return FactionAbilityResponse.fail(gameId, code, "전진할 트랙을 지정해야 합니다");

        // 연구소 확인 및 교역소로 다운그레이드
        Optional<GameBuilding> bOpt = buildingRepository.findByGameIdAndHexQAndHexR(gameId, req.hexQ(), req.hexR());
        if (bOpt.isEmpty() || !bOpt.get().getPlayerId().equals(playerId))
            return FactionAbilityResponse.fail(gameId, code, "해당 위치에 본인 건물이 없습니다");
        if (bOpt.get().getBuildingType() != BuildingType.RESEARCH_LAB)
            return FactionAbilityResponse.fail(gameId, code, "연구소만 다운그레이드 가능합니다");

        GameBuilding rl = bOpt.get();
        rl.upgrade(BuildingType.TRADING_STATION); // 다운그레이드: RL → TS
        buildingRepository.save(rl);

        // 재고 조정: RL +1 반환, TS -1 사용
        ps.addResearchLabToStock();
        ps.decreaseStockTradingStation();

        // 지식 트랙 1칸 전진 (지식 소모 없음)
        ps.advanceTechTrackNoKnowledge(trackCodeToField(req.trackCode()));
        ps.markFactionAbilityUsed();
        playerStateRepository.save(ps);
        log.info("[FIRAKS PI] RL→TS 다운그레이드 + {} 전진: player={}", req.trackCode(), playerId);

        var result = actionService.saveActionAndNextTurn(gameId, playerId, ActionType.FACTION_ABILITY,
                "{\"abilityCode\":\"" + code + "\",\"track\":\"" + req.trackCode() + "\"}");
        return FactionAbilityResponse.success(gameId, code, result.nextTurnSeatNo());
    }

    /**
     * 엠바스 PI: 광산 위치 ↔ 의회 위치 교환 (라운드당 1회, 파워 리치 없음)
     * request.hexQ/hexR = 교환할 광산 위치
     */
    private FactionAbilityResponse handleAmbasPiSwap(UUID gameId, UUID playerId, GamePlayerState ps, String code, FactionAbilityRequest req) {
        if (ps.getFactionType() != FactionType.AMBAS) return FactionAbilityResponse.fail(gameId, code, "엠바스만 사용 가능");
        if (!hasPi(ps)) return FactionAbilityResponse.fail(gameId, code, "행성 의회 건설 후 사용 가능합니다");
        if (ps.isFactionAbilityUsed()) return FactionAbilityResponse.fail(gameId, code, "이번 라운드 이미 사용했습니다");
        if (req.hexQ() == null || req.hexR() == null) return FactionAbilityResponse.fail(gameId, code, "교환할 광산 위치를 지정해야 합니다");

        // 광산 확인
        Optional<GameBuilding> mineOpt = buildingRepository.findByGameIdAndHexQAndHexR(gameId, req.hexQ(), req.hexR());
        if (mineOpt.isEmpty() || !mineOpt.get().getPlayerId().equals(playerId) || mineOpt.get().getBuildingType() != BuildingType.MINE)
            return FactionAbilityResponse.fail(gameId, code, "해당 위치에 본인 광산이 없습니다");

        // 의회 확인
        List<GameBuilding> piList = buildingRepository.findByGameIdAndPlayerId(gameId, playerId).stream()
                .filter(b -> b.getBuildingType() == BuildingType.PLANETARY_INSTITUTE)
                .toList();
        if (piList.isEmpty()) return FactionAbilityResponse.fail(gameId, code, "행성 의회를 찾을 수 없습니다");

        GameBuilding mine = mineOpt.get();
        GameBuilding pi = piList.get(0);

        // 위치 교환 (각 건물의 Q,R 교환)
        int mq = mine.getHexQ(), mr = mine.getHexR();
        int pq = pi.getHexQ(), pr = pi.getHexR();
        swapBuildingPosition(mine, pq, pr);
        swapBuildingPosition(pi, mq, mr);
        buildingRepository.save(mine);
        buildingRepository.save(pi);

        ps.markFactionAbilityUsed();
        playerStateRepository.save(ps);
        log.info("[AMBAS PI] 광산({},{}) ↔ 의회({},{}) 교환: player={}", mq, mr, pq, pr, playerId);

        var result = actionService.saveActionAndNextTurn(gameId, playerId, ActionType.FACTION_ABILITY,
                "{\"abilityCode\":\"" + code + "\"}");
        return FactionAbilityResponse.success(gameId, code, result.nextTurnSeatNo());
    }

    /**
     * 하드쉬 할라 PI: 크레딧으로 자원 변환 (프리 액션)
     * trackCode: ORE(4c→1o), KNOWLEDGE(2c→1k), QIC(3c→1qic)
     */
    private FactionAbilityResponse handleHadschHallasCreditConvert(UUID gameId, UUID playerId, GamePlayerState ps, String code, FactionAbilityRequest req) {
        if (ps.getFactionType() != FactionType.HADSCH_HALLAS) return FactionAbilityResponse.fail(gameId, code, "하드쉬 할라만 사용 가능");
        if (!hasPi(ps)) return FactionAbilityResponse.fail(gameId, code, "행성 의회 건설 후 사용 가능합니다");

        String target = req.trackCode(); // ORE / KNOWLEDGE / QIC
        switch (target == null ? "" : target) {
            case "ORE" -> {
                ps.spendCredit(4);
                ps.addOre(1);
                log.info("[HADSCH_HALLAS PI] 4크레딧→1광석: player={}", playerId);
            }
            case "KNOWLEDGE" -> {
                ps.spendCredit(2);
                ps.addKnowledge(1);
                log.info("[HADSCH_HALLAS PI] 2크레딧→1지식: player={}", playerId);
            }
            case "QIC" -> {
                ps.spendCredit(3);
                ps.addQic(1);
                log.info("[HADSCH_HALLAS PI] 3크레딧→1QIC: player={}", playerId);
            }
            default -> { return FactionAbilityResponse.fail(gameId, code, "trackCode: ORE/KNOWLEDGE/QIC 중 하나를 지정하세요"); }
        }
        playerStateRepository.save(ps);
        return FactionAbilityResponse.success(gameId, code, null); // 프리 액션: 턴 소모 없음
    }

    /**
     * 글린 PI: 2크레딧+1광석+1지식 → 연방 토큰 획득 (액션)
     */
    private FactionAbilityResponse handleGleensFederationToken(UUID gameId, UUID playerId, GamePlayerState ps, String code) {
        if (ps.getFactionType() != FactionType.GLEENS) return FactionAbilityResponse.fail(gameId, code, "글린만 사용 가능");
        if (!hasPi(ps)) return FactionAbilityResponse.fail(gameId, code, "행성 의회 건설 후 사용 가능합니다");
        if (ps.isFactionAbilityUsed()) return FactionAbilityResponse.fail(gameId, code, "이번 라운드 이미 사용했습니다");

        ps.spendCredit(2);
        ps.spendOre(1);
        ps.spendKnowledge(1);

        // 기본 연방 토큰 중 하나 부여 (임의: FEDERATION_7VP)
        federationTokenRepository.save(GamePlayerFederationToken.builder()
                .gameId(gameId)
                .playerId(playerId)
                .federationTileType(FederationTileType.GLEENS_FEDERATION)
                .build());

        ps.markFactionAbilityUsed();
        playerStateRepository.save(ps);
        log.info("[GLEENS PI] 2c+1o+1k → 연방 토큰: player={}", playerId);

        var result = actionService.saveActionAndNextTurn(gameId, playerId, ActionType.FACTION_ABILITY,
                "{\"abilityCode\":\"" + code + "\"}");
        return FactionAbilityResponse.success(gameId, code, result.nextTurnSeatNo());
    }

    /**
     * 아이타 PI: 4 가이아파워 영구 제거 → 기본 기술 타일 획득 (FE에서 타일 선택)
     * 이 메서드는 "가이아 토큰 차감" 처리만 함. 타일 획득은 별도 API로 처리.
     */
    private FactionAbilityResponse handleItarsGaiaToTechTile(UUID gameId, UUID playerId, GamePlayerState ps, String code) {
        if (ps.getFactionType() != FactionType.ITARS) return FactionAbilityResponse.fail(gameId, code, "아이타만 사용 가능");
        if (!hasPi(ps)) return FactionAbilityResponse.fail(gameId, code, "행성 의회 건설 후 사용 가능합니다");
        if (ps.getGaiaPower() < 4) return FactionAbilityResponse.fail(gameId, code, "가이아 구역 파워가 4개 미만입니다 (현재: " + ps.getGaiaPower() + ")");

        // 4 가이아파워 영구 제거
        ps.removeGaiaPower(4);
        playerStateRepository.save(ps);
        log.info("[ITARS PI] 가이아 4 제거 → 기본 기술타일 선택 대기: player={}", playerId);
        // 타일 선택은 FE에서 TechTilePickerPanel을 통해 처리
        return FactionAbilityResponse.success(gameId, code, null); // 프리 액션 (타일 선택 후 확정)
    }

    // ═══════════════════════════════════════════════════════════
    // 유틸
    // ═══════════════════════════════════════════════════════════

    private String trackCodeToField(String trackCode) {
        return switch (trackCode) {
            case "TERRA_FORMING"  -> "techTerraforming";
            case "NAVIGATION"     -> "techNavigation";
            case "AI"             -> "techAi";
            case "GAIA_FORMING"   -> "techGaia";
            case "ECONOMY"        -> "techEconomy";
            case "SCIENCE"        -> "techScience";
            default -> throw new IllegalArgumentException("알 수 없는 트랙 코드: " + trackCode);
        };
    }

    /** 건물 좌표 교환 (reflection 없이 직접 setter가 없으므로 GameBuilding에 swapPosition 추가 필요) */
    private void swapBuildingPosition(GameBuilding building, int newQ, int newR) {
        building.setPosition(newQ, newR);
    }
}
