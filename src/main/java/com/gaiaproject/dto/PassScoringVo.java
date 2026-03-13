package com.gaiaproject.dto;

import java.util.function.ToIntFunction;

/**
 * 패스 시 점수 계산 VO
 * - 고급 기술 타일 중 "패스 후 X당 Y VP" 능력 계산용
 * - 예: "패스 후 연구소 1개당 3VP", "패스 후 행성 종류 1개당 1VP"
 */
public final class PassScoringVo {

    // 점수 계산 함수
    private final ToIntFunction<PassContextVo> scorer;

    private PassScoringVo(ToIntFunction<PassContextVo> scorer) {
        this.scorer = scorer;
    }

    /**
     * 실제 점수 계산
     * @param ctx 플레이어의 현재 게임 상태
     * @return 계산된 VP
     */
    public int score(PassContextVo ctx) {
        return scorer.applyAsInt(ctx);
    }

    // ========== 점수 계산 팩토리 메서드 ==========

    /**
     * 패스 점수 없음 (0점)
     */
    public static PassScoringVo none() {
        return new PassScoringVo(ctx -> 0);
    }

    /**
     * 광산 1개당 X VP
     * 예: ADV_TILE_14 "즉발: 광산 1개당 2VP"
     */
    public static PassScoringVo perMine(int vp) {
        return new PassScoringVo(ctx -> ctx.mines() * vp);
    }

    /**
     * 교역소 1개당 X VP
     * 예: ADV_TILE_15 "즉발: 교역소 1개당 4VP"
     */
    public static PassScoringVo perTradingStation(int vp) {
        return new PassScoringVo(ctx -> ctx.tradingStations() * vp);
    }

    /**
     * 연구소 1개당 X VP
     * 예: ADV_TILE_5 "패시브: 패스 후 연구소 1개당 3VP"
     */
    public static PassScoringVo perResearchLab(int vp) {
        return new PassScoringVo(ctx -> ctx.researchLabs() * vp);
    }

    /**
     * 행성 연구소 + 아카데미 1개당 X VP
     * (PI: Planetary Institute)
     */
    public static PassScoringVo perPiOrAcademy(int vp) {
        return new PassScoringVo(ctx -> (ctx.planetaryInstitutes() + ctx.academies()) * vp);
    }

    /**
     * 가이아 행성 1개당 X VP
     * 예: ADV_TILE_17 "즉발: 가이아 땅 1개당 2VP"
     */
    public static PassScoringVo perGaiaPlanet(int vp) {
        return new PassScoringVo(ctx -> ctx.gaiaPlanets() * vp);
    }

    /**
     * 개척한 행성 종류 1개당 X VP
     * 예: ADV_TILE_6 "패시브: 패스 후 행성 종류 1개당 1VP"
     */
    public static PassScoringVo perPlanetTypeKind(int vp) {
        return new PassScoringVo(ctx -> ctx.colonizedPlanetTypeKinds() * vp);
    }

    /**
     * 가이아포머 1개당 X VP
     */
    public static PassScoringVo perGaiaformer(int vp) {
        return new PassScoringVo(ctx -> ctx.gaiaformers() * vp);
    }

    /**
     * 깊은 구역(Lost Planet) 건물 1개당 X VP
     * 예: ADV_TILE_2 "패시브: 패스 후 깊은 구역 타일 1개당 2VP"
     */
    public static PassScoringVo perDeepSectorStructure(int vp) {
        return new PassScoringVo(ctx -> ctx.deepSectorStructures() * vp);
    }
}