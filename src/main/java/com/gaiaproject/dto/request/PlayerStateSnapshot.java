package com.gaiaproject.dto.request;

/**
 * 턴 확정 시 FE가 계산한 플레이어 최종 상태 스냅샷.
 *
 * 모든 필드 nullable — null이면 기존 DB 값 유지.
 * 제공된 필드만 DB에 덮어씌움.
 */
public record PlayerStateSnapshot(
        // 자원
        Integer credit,
        Integer ore,
        Integer knowledge,
        Integer qic,

        // 파워
        Integer powerBowl1,
        Integer powerBowl2,
        Integer powerBowl3,
        Integer brainstoneBowl,      // 타클론 전용, null = 없음, 1/2/3 = 해당 bowl
        Integer gaiaPower,

        // VP
        Integer victoryPoints,

        // 기술 트랙 레벨
        Integer techTerraforming,
        Integer techNavigation,
        Integer techAi,
        Integer techGaia,
        Integer techEconomy,
        Integer techScience,

        // 건물 재고
        Integer stockMine,
        Integer stockTradingStation,
        Integer stockResearchLab,
        Integer stockPlanetaryInstitute,
        Integer stockAcademy,
        Integer stockGaiaformer,

        // 종족/페이즈 플래그
        Boolean boosterActionUsed,
        Boolean factionAbilityUsed,
        Boolean qicAcademyActionUsed,
        Boolean gleensHasQicAcademy,

        // 발타크: QIC로 변환된 가이아포머 수
        Integer baltaksConvertedGaiaformers,
        // 가이아포머 영구 제거
        Integer permanentlyRemovedGaiaformers,

        // 연방 카운터
        Integer federationCount,

        // 팅커로이드 액션 타일 선택 기록
        String tinkeroidsUsedActions,
        String tinkeroidsCurrentAction
) {}
