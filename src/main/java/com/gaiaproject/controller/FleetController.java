package com.gaiaproject.controller;

import com.gaiaproject.dto.response.FleetOccupancyResponse;
import com.gaiaproject.dto.response.FleetProbeStatusResponse;
import com.gaiaproject.service.FleetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@Tag(name = "Fleet", description="함대 관련 API")
@RestController
@RequestMapping("/api/rooms/{roomId}/fleet")
@RequiredArgsConstructor
public class FleetController {

    private final FleetService fleetService;

    @Operation(summary = "탐사선 상태 조회")
    @GetMapping("/probes")
    public ResponseEntity<FleetProbeStatusResponse> getFleetProbeStatus(
            @PathVariable UUID roomId,
            @RequestParam UUID playerId
    ) {
        log.info("탐사선 상태 조회: roomId={}, playerId={}", roomId, playerId);
        FleetProbeStatusResponse response = fleetService.getFleetProbeStatus(roomId, playerId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "전체 함대 점유 현황 조회")
    @GetMapping("/occupancy")
    public ResponseEntity<FleetOccupancyResponse> getFleetOccupancy(@PathVariable UUID roomId) {
        log.info("함대 점유 현황 조회: roomId={}", roomId);
        FleetOccupancyResponse response = fleetService.getAllFleetOccupancy(roomId);
        return ResponseEntity.ok(response);
    }
}
