package com.gaiaproject.dto.response;

import java.util.UUID;

public record DeployGaiaformerResponse(
        UUID gameId,
        boolean success,
        String message,
        int hexQ,
        int hexR,
        int nextTurnSeatNo
) {
    public static DeployGaiaformerResponse success(UUID gameId, int hexQ, int hexR, int nextTurnSeatNo) {
        return new DeployGaiaformerResponse(gameId, true, "포머 배치 완료", hexQ, hexR, nextTurnSeatNo);
    }

    public static DeployGaiaformerResponse fail(UUID gameId, String message) {
        return new DeployGaiaformerResponse(gameId, false, message, 0, 0, 0);
    }
}
