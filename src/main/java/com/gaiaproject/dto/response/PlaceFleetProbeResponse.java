package com.gaiaproject.dto.response;

import java.util.List;
import java.util.UUID;

public record PlaceFleetProbeResponse(
        UUID gameId,
        UUID playerId,
        boolean success,
        String message,
        String fleetName,
        List<String> unlockedActions,  // 활성화된 함대 액션 목록
        UUID actionId  // PENDING 액션 ID (확정/취소에 사용)
) {
    public static PlaceFleetProbeResponse success(UUID gameId, UUID playerId, String fleetName,
                                                   List<String> unlockedActions, UUID actionId) {
        return new PlaceFleetProbeResponse(gameId, playerId, true, null, fleetName, unlockedActions, actionId);
    }

    public static PlaceFleetProbeResponse fail(UUID gameId, UUID playerId, String message) {
        return new PlaceFleetProbeResponse(gameId, playerId, false, message, null, null, null);
    }
}
