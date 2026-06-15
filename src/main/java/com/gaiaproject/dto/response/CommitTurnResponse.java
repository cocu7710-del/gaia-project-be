package com.gaiaproject.dto.response;

import java.util.UUID;

public record CommitTurnResponse(
        UUID gameId,
        boolean success,
        String message,
        Integer nextTurnSeatNo,
        boolean hasLeech,
        String leechBatchKey,
        GameSnapshot snapshot
) {
    public static CommitTurnResponse success(UUID gameId, Integer nextTurnSeatNo, boolean hasLeech, String leechBatchKey, GameSnapshot snapshot) {
        return new CommitTurnResponse(gameId, true, null, nextTurnSeatNo, hasLeech, leechBatchKey, snapshot);
    }

    public static CommitTurnResponse fail(UUID gameId, String message) {
        return new CommitTurnResponse(gameId, false, message, null, false, null, null);
    }
}
