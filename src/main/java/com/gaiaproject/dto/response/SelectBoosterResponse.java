package com.gaiaproject.dto.response;

import java.util.UUID;

public record SelectBoosterResponse(
        UUID gameId,
        boolean success,
        String message,
        int nextSeatNo  // 다음 선택할 좌석 번호 (0이면 모두 완료)
) {
    public static SelectBoosterResponse success(UUID gameId, int nextSeatNo) {
        return new SelectBoosterResponse(gameId, true, null, nextSeatNo);
    }

    public static SelectBoosterResponse fail(UUID gameId, String message) {
        return new SelectBoosterResponse(gameId, false, message, -1);
    }
}
