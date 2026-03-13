package com.gaiaproject.dto;

/**
 * 기술 트랙 초기값 VO
 * - 6개 트랙: 테라포밍, 항해, AI, 가이아, 경제, 과학
 */
public record TechTracksVo(
        int terraforming,
        int navigation,
        int ai,
        int gaia,
        int economy,
        int science
) {
    public static TechTracksVo zero() {
        return new TechTracksVo(0, 0, 0, 0, 0, 0);
    }
}
