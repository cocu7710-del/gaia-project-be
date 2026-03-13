package com.gaiaproject.dto.request;

import java.util.UUID;

public record AdvanceTechRequest(
        UUID playerId,
        String trackCode  // TERRA_FORMING, NAVIGATION, AI, GAIA_FORMING, ECONOMY, SCIENCE
) {}
