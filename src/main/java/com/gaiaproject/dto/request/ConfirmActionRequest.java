package com.gaiaproject.dto.request;

import java.util.UUID;

public record ConfirmActionRequest(
        UUID actionId,
        UUID playerId,
        String actionType,
        String actionData
) {
}
