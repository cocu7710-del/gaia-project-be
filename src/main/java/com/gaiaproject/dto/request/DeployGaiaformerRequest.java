package com.gaiaproject.dto.request;

import java.util.UUID;

public record DeployGaiaformerRequest(
        UUID playerId,
        int hexQ,
        int hexR,
        int qicUsed,     // 항법 거리 확장에 사용한 QIC
        boolean isInstant // true: 즉시 포밍 (파워 차감 없이 즉시 GAIA 변환)
) {}
