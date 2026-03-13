package com.gaiaproject.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "게임 시작 응답")
public record StartGameResponse(
        UUID gameId,
        boolean success,
        String message,
        Integer currentRound,
        Integer currentTurnSeatNo,

        @Schema(description = "게임 페이즈 (SETUP_MINE_FIRST, SETUP_MINE_SECOND, PLAYING)")
        String gamePhase,

        @Schema(description = "초기 광산 배치 시 다음 배치할 좌석 번호")
        Integer nextSetupSeatNo
) {
    public static StartGameResponse success(UUID gameId, int round, int turnSeatNo) {
        return new StartGameResponse(gameId, true, null, round, turnSeatNo, "PLAYING", null);
    }

    public static StartGameResponse fail(UUID gameId, String message) {
        return new StartGameResponse(gameId, false, message, null, null, null, null);
    }

    public static StartGameResponse startMinePlacement(UUID gameId, int nextSetupSeatNo, String gamePhase) {
        return new StartGameResponse(gameId, true, "초기 광산 배치 단계 시작", 1, null, gamePhase, nextSetupSeatNo);
    }
}
