package com.gaiaproject.controller;

import com.gaiaproject.domain.entity.building.GameBuilding;
import com.gaiaproject.dto.request.PlaceInitialMineRequest;
import com.gaiaproject.dto.response.PlaceInitialMineResponse;
import com.gaiaproject.service.BuildingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Slf4j
@Tag(name = "Building", description = "건물 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms/{roomId}")
public class BuildingController {

    private final BuildingService buildingService;

    @Operation(summary = "초기 광산 배치", description = "게임 시작 전 초기 광산 배치 (순서: 1→2→3→4→4→3→2→1)")
    @PostMapping("/buildings/mine/initial")
    public ResponseEntity<PlaceInitialMineResponse> placeInitialMine(
            @PathVariable UUID roomId,
            @Valid @RequestBody PlaceInitialMineRequest request
    ) {
        PlaceInitialMineResponse response = buildingService.placeInitialMine(roomId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "게임 내 모든 건물 조회")
    @GetMapping("/buildings")
    public ResponseEntity<List<GameBuilding>> getAllBuildings(@PathVariable UUID roomId) {
        List<GameBuilding> buildings = buildingService.getBuildingsByGame(roomId);
        return ResponseEntity.ok(buildings);
    }

    @Operation(summary = "특정 플레이어의 건물 조회")
    @GetMapping("/buildings/player/{playerId}")
    public ResponseEntity<List<GameBuilding>> getPlayerBuildings(
            @PathVariable UUID roomId,
            @PathVariable UUID playerId
    ) {
        List<GameBuilding> buildings = buildingService.getBuildingsByPlayer(roomId, playerId);
        return ResponseEntity.ok(buildings);
    }
}
