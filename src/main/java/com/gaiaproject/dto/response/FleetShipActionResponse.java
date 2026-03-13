package com.gaiaproject.dto.response;

import java.util.UUID;

public record FleetShipActionResponse(
        UUID gameId,
        boolean success,
        String message,
        String actionCode,
        Integer gainedVP,
        Integer nextTurnSeatNo,
        boolean turnEnded   // 즉시 액션은 true, 후속 액션(mine/upgrade) 필요하면 false
) {
    public static FleetShipActionResponse success(UUID gameId, String actionCode, int gainedVP,
                                                   Integer nextTurnSeatNo, boolean turnEnded) {
        return new FleetShipActionResponse(gameId, true, null, actionCode, gainedVP, nextTurnSeatNo, turnEnded);
    }

    public static FleetShipActionResponse fail(UUID gameId, String actionCode, String message) {
        return new FleetShipActionResponse(gameId, false, message, actionCode, 0, null, false);
    }
}
