package com.gaiaproject.dto.request;

import java.util.UUID;

public record PlaceFleetProbeRequest(
        UUID playerId,
        String fleetName,  // TF_MARS, ECLIPSE, TWILIGHT, REBELLION
        int qicUsed        // 항법 거리 초과 시 소모한 QIC
) {
}
