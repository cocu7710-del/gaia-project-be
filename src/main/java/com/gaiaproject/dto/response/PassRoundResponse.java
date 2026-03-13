package com.gaiaproject.dto.response;

import java.util.UUID;

public record PassRoundResponse(
        UUID gameId,
        UUID playerId,
        boolean success,
        String message,
        Integer roundNumber,
        Integer nextTurnSeatNo,  // 다음 턴 좌석 번호 (0이면 라운드 종료)
        boolean allPassed
) {
    public static PassRoundResponse success(UUID gameId, UUID playerId, int roundNumber,
                                           int nextTurnSeatNo, boolean allPassed) {
        return new PassRoundResponse(gameId, playerId, true, null, roundNumber, nextTurnSeatNo, allPassed);
    }

    public static PassRoundResponse fail(UUID gameId, UUID playerId, String message) {
        return new PassRoundResponse(gameId, playerId, false, message, null, null, false);
    }
}
