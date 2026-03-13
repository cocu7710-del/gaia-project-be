package com.gaiaproject.controller;

import com.gaiaproject.domain.enumtype.booster.BoosterActionType;
import com.gaiaproject.dto.response.BoosterOfferResponse;
import com.gaiaproject.service.RoundBoosterService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Tag(name = "Round", description = "라운드 관련 API")
@RestController
@RequiredArgsConstructor
public class RoundBooterController {

    private final RoundBoosterService roundBoosterService;

    @Operation(summary = "게임 라운드 부스터 조회")
    @GetMapping("/api/round/{roomId}/booster")
    public ResponseEntity<List<BoosterOfferResponse>> getRoundBooster(@PathVariable UUID roomId) {
        List<BoosterOfferResponse> boosters = roundBoosterService.getBoosters(roomId).stream()
                .map(BoosterOfferResponse::from)
                .toList();

        return ResponseEntity.ok(boosters);
    }

    @Operation(summary = "부스터 액션 사용 (라운드당 1회)")
    @PostMapping("/api/round/{roomId}/booster/action")
    public ResponseEntity<?> useBoosterAction(
            @PathVariable UUID roomId,
            @RequestBody Map<String, String> body) {
        UUID playerId = UUID.fromString(body.get("playerId"));
        try {
            BoosterActionType actionType = roundBoosterService.useBoosterAction(roomId, playerId);
            return ResponseEntity.ok(Map.of("success", true, "actionType", actionType.name()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
