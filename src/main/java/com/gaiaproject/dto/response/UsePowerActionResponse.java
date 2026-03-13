package com.gaiaproject.dto.response;

import java.util.UUID;

public record UsePowerActionResponse(
        UUID gameId,
        boolean success,
        String message,
        String powerActionCode,
        Integer nextTurnSeatNo
) {
    public static UsePowerActionResponse success(UUID gameId, String code, int nextSeatNo) {
        return new UsePowerActionResponse(gameId, true, null, code, nextSeatNo);
    }

    public static UsePowerActionResponse fail(UUID gameId, String code, String message) {
        return new UsePowerActionResponse(gameId, false, message, code, null);
    }
}
