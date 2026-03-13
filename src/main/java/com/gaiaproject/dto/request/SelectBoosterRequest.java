package com.gaiaproject.dto.request;

import java.util.UUID;

public record SelectBoosterRequest(
        UUID playerId,
        String boosterCode
) {}
