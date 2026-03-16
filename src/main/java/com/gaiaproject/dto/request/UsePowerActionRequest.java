package com.gaiaproject.dto.request;

import java.util.UUID;

public record UsePowerActionRequest(
        UUID playerId,
        String powerActionCode,  // PWR_KNOWLEDGE, PWR_ORE_2, etc.
        Boolean useBrainstone    // 타클론 전용: 브레인스톤 사용 여부
) {}
