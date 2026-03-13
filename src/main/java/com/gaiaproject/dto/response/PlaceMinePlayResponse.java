package com.gaiaproject.dto.response;

import java.util.UUID;

public record PlaceMinePlayResponse(
        UUID gameId,
        boolean success,
        String message,
        int hexQ,
        int hexR,
        Integer nextTurnSeatNo
) {
    public static PlaceMinePlayResponse success(UUID gameId, int hexQ, int hexR, int nextSeatNo) {
        return new PlaceMinePlayResponse(gameId, true, null, hexQ, hexR, nextSeatNo);
    }

    public static PlaceMinePlayResponse fail(UUID gameId, String message) {
        return new PlaceMinePlayResponse(gameId, false, message, 0, 0, null);
    }
}
