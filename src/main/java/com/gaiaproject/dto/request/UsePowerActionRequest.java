package com.gaiaproject.dto.request;

import java.util.UUID;

public record UsePowerActionRequest(
        UUID playerId,
        String powerActionCode  // PWR_KNOWLEDGE, PWR_ORE_2, etc.
) {}
