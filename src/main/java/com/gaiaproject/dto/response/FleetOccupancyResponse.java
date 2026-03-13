package com.gaiaproject.dto.response;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public record FleetOccupancyResponse(
        UUID gameId,
        Map<String, List<String>> probesByFleet  // fleetName -> ordered list of playerIds (entry order)
) {
}
