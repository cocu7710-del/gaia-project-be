package com.gaiaproject.dto.response;

import java.util.UUID;

public record ConfirmActionResponse(
        UUID gameId,
        UUID actionId,
        boolean success,
        String message,
        Integer nextTurnSeatNo,  // 다음 턴 좌석 번호 (0이면 라운드 종료)
        boolean roundEnded
) {
    public static ConfirmActionResponse success(UUID gameId, UUID actionId, int nextTurnSeatNo, boolean roundEnded) {
        return new ConfirmActionResponse(gameId, actionId, true, null, nextTurnSeatNo, roundEnded);
    }

    public static ConfirmActionResponse fail(UUID gameId, UUID actionId, String message) {
        return new ConfirmActionResponse(gameId, actionId, false, message, null, false);
    }
}
