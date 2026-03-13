package com.gaiaproject.dto.response;

import java.util.UUID;

public record UpgradeBuildingResponse(
        UUID gameId,
        boolean success,
        String message,
        int hexQ,
        int hexR,
        String fromBuildingType,
        String toBuildingType,
        Integer nextTurnSeatNo
) {
    public static UpgradeBuildingResponse success(UUID gameId, int hexQ, int hexR,
            String from, String to, int nextSeatNo) {
        return new UpgradeBuildingResponse(gameId, true, null, hexQ, hexR, from, to, nextSeatNo);
    }

    public static UpgradeBuildingResponse fail(UUID gameId, String message) {
        return new UpgradeBuildingResponse(gameId, false, message, 0, 0, null, null, null);
    }
}
