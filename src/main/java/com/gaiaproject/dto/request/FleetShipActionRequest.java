package com.gaiaproject.dto.request;

import java.util.UUID;

public record FleetShipActionRequest(
        UUID playerId,
        String actionCode,
        Integer hexQ,       // 광산 배치 / 건물 업그레이드 좌표 (nullable)
        Integer hexR,
        String trackCode,   // 기술 타일 코드 또는 트랙 코드 (nullable)
        String techTrackCode // REBELLION_TECH: 타일 획득 시 전진할 트랙 코드 (nullable)
) {}
