package com.gaiaproject.dto.request;

import java.util.List;
import java.util.UUID;

/**
 * 턴 확정 요청 (C안 commit-turn).
 *
 * FE가 한 턴 동안 발생한 모든 변경을 1회 요청으로 전송.
 * BE는 규칙 검증 없이 받은 값 그대로 DB에 반영.
 *
 * - playerState: 덮어쓸 최종 플레이어 상태 (부분 필드 허용, null이면 유지)
 * - newBuildings: INSERT 할 건물들
 * - upgradedBuildings: UPDATE 할 건물 타입 변경들
 * - newTechTiles: INSERT 할 기술타일
 * - coveredTiles: UPDATE 할 기본타일 (고급 타일 커버)
 * - hexChanges: UPDATE 할 헥스 행성 타입 (테라포밍/블랙행성)
 * - flippedFederationTokens: 사용된 연방 토큰 코드들
 * - newFederationGroups: 새로 형성된 연방
 * - actionLog: 액션 로그 엔트리 (감사/재생용)
 * - vpLog: VP 로그 엔트리
 * - leechTargets: 리치 오퍼 생성 대상
 * - passNextBooster: 라운드 패스 시 선택한 다음 부스터 (null이면 패스 아님)
 */
public record CommitTurnRequest(
        UUID playerId,

        // 플레이어 상태 덮어쓰기 (부분 필드 허용, null이면 기존 값 유지)
        PlayerStateSnapshot playerState,

        // 건물 변경
        List<NewBuilding> newBuildings,
        List<UpgradedBuilding> upgradedBuildings,

        // 헥스 변경 (테라포밍, 블랙행성)
        List<HexChange> hexChanges,

        // 기술 타일 변경
        List<NewTechTile> newTechTiles,
        List<CoveredTile> coveredTiles,

        // 연방 변경
        List<NewFederationGroup> newFederationGroups,
        List<String> flippedFederationTokens,

        // 인공물 획득 (TWILIGHT_ARTIFACT)
        List<String> newArtifacts,

        // 건물 특수 상태
        List<HexCoord> ringedBuildings,        // MOWEIDS_RING: 건물에 링 추가
        List<HexCoord> downgradedToTs,         // FIRAKS_DOWNGRADE: RL → TS
        List<HexCoord> gaiaformerReturns,      // 가이아포머 배치된 GAIA 행성에 광산 건설 → GAIAFORMER → MINE upgrade + stock 반환
        AmbasSwap ambasSwap,                   // AMBAS_SWAP: PI ↔ Mine 교환 (좌표 2개)

        // 함대 입장 (FLEET_PROBE)
        String fleetProbeName,                 // 입장할 함대 이름 (nullable)

        // 라운드 진행
        String passNextBooster,

        // 이번 턴에 사용한 기술타일 액션 코드들 (라운드 1회 제한 플래그 용)
        List<String> usedTechTileActions,

        // 이번 턴에 사용한 공용 파워 액션 코드들 (PWR_*) — GamePowerActionUsage INSERT 용
        List<String> usedCommonPowerActions,

        // 로그
        List<ActionLogEntry> actionLog,
        List<VpLogEntry> vpLog,

        // 리치
        List<LeechTarget> leechTargets
) {
    public record NewBuilding(
            int hexQ,
            int hexR,
            String buildingType,  // MINE, TRADING_STATION, RESEARCH_LAB, PLANETARY_INSTITUTE, ACADEMY, GAIAFORMER, LOST_PLANET_MINE, SPACE_STATION
            Boolean isLantidsMine
    ) {}

    public record UpgradedBuilding(
            int hexQ,
            int hexR,
            String newBuildingType,
            String academyType  // ACADEMY 업그레이드 시 KNOWLEDGE / QIC 중 하나 (그 외 null)
    ) {}

    public record HexChange(
            int hexQ,
            int hexR,
            String newPlanetType
    ) {}

    public record NewTechTile(
            String tileCode,
            String trackCode  // nullable (COMMON/EXPANSION 타일은 별도)
    ) {}

    public record CoveredTile(
            String tileCode,
            String coveredByCode
    ) {}

    public record NewFederationGroup(
            String tileCode,
            List<int[]> buildingHexes,
            List<int[]> tokenHexes
    ) {}

    public record ActionLogEntry(
            String actionType,
            String actionData
    ) {}

    public record VpLogEntry(
            String category,
            int amount,
            String description
    ) {}

    public record LeechTarget(
            UUID receivePlayerId,
            int power,
            int vpCost,
            int chargeable,
            boolean isTaklons
    ) {}

    public record HexCoord(int hexQ, int hexR) {}

    public record AmbasSwap(
            int piHexQ, int piHexR,
            int mineHexQ, int mineHexR
    ) {}
}
