package com.gaiaproject.dto.request;

import java.util.UUID;

public record LeechDecideRequest(
    UUID playerId,
    boolean accept,
    String taklonsChoice  // "TOKEN_FIRST" or "CHARGE_FIRST", only for Taklons-PI
) {}
