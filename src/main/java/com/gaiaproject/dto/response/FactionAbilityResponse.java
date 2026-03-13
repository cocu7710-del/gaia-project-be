package com.gaiaproject.dto.response;

import java.util.UUID;

/**
 * 종족 고유 능력 액션 응답
 */
public record FactionAbilityResponse(
        UUID gameId,
        boolean success,
        String message,
        String abilityCode,
        Integer nextTurnSeatNo
) {
    public static FactionAbilityResponse success(UUID gameId, String abilityCode, Integer nextTurnSeatNo) {
        return new FactionAbilityResponse(gameId, true, null, abilityCode, nextTurnSeatNo);
    }

    public static FactionAbilityResponse fail(UUID gameId, String abilityCode, String message) {
        return new FactionAbilityResponse(gameId, false, message, abilityCode, null);
    }
}
