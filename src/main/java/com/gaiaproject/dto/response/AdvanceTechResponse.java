package com.gaiaproject.dto.response;

import java.util.UUID;

public record AdvanceTechResponse(
        UUID gameId,
        boolean success,
        String message,
        String trackCode,
        int newLevel,
        Integer nextTurnSeatNo
) {
    public static AdvanceTechResponse success(UUID gameId, String trackCode, int newLevel, int nextTurnSeatNo) {
        return new AdvanceTechResponse(gameId, true, null, trackCode, newLevel, nextTurnSeatNo);
    }

    public static AdvanceTechResponse fail(UUID gameId, String message) {
        return new AdvanceTechResponse(gameId, false, message, null, 0, null);
    }
}
