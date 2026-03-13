package com.gaiaproject.dto.request;

import java.util.UUID;

public record FleetShipActionRequest(
        UUID playerId,
        String actionCode,
        Integer hexQ,       // 광산 배치 / 건물 업그레이드 좌표 (nullable)
        Integer hexR,
        String trackCode    // 기술 트랙 전진 (nullable)
) {}
