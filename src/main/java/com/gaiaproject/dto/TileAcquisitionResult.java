package com.gaiaproject.dto;

/**
 * 기술타일 획득 결과 — 후속 액션 필요 여부
 */
public record TileAcquisitionResult(
    boolean needsMine,        // BASIC_EXP_TILE_3: 2삽 광산 배치 필요
    boolean needsLostPlanet   // NAV 4→5: 검은행성 배치 필요
) {
    public static TileAcquisitionResult none() {
        return new TileAcquisitionResult(false, false);
    }
}
