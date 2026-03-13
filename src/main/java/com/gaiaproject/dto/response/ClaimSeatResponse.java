package com.gaiaproject.dto.response;

import java.util.UUID;

public record ClaimSeatResponse(
        UUID gameId,
        boolean success,
        String message,
        GamePublicStateResponse publicState
) {
    public static ClaimSeatResponse success(UUID gameId, GamePublicStateResponse publicState) {
        return new ClaimSeatResponse(gameId, true, null, publicState);
    }

    public static ClaimSeatResponse fail(UUID gameId, String message) {
        return new ClaimSeatResponse(gameId, false, message, null);
    }
}
