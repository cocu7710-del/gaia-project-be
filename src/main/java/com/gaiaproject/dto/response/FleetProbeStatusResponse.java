package com.gaiaproject.dto.response;

import java.util.Map;
import java.util.UUID;

public record FleetProbeStatusResponse(
        UUID gameId,
        UUID playerId,
        Map<String, Boolean> probeStatus  // 함대명 -> 배치 여부
) {
}
