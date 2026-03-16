package com.gaiaproject.dto.response;

import java.util.List;

/**
 * 연방 타일 응답 DTO
 */
public record FederationTilesResponse(
        List<FederationTileInfo> generalSupply,
        FederationTileInfo terraformingTrackTile,
        List<FederationTileInfo> forgottenFleet,
        List<ArtifactInfo> artifacts
) {
    /**
     * 연방 타일 정보
     */
    public record FederationTileInfo(
            String tileCode,
            String description,
            int quantity,
            Integer position
    ) {}

    /**
     * 인공물 정보 (트와일라잇 슬롯 4개)
     */
    public record ArtifactInfo(
            String artifactCode,
            String description,
            int position,
            boolean isTaken,
            String acquiredByPlayerId
    ) {}
}
