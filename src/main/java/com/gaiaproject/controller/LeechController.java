package com.gaiaproject.controller;

import com.gaiaproject.domain.entity.leech.GameLeechOffer;
import com.gaiaproject.dto.request.LeechDecideRequest;
import com.gaiaproject.service.PowerLeechService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Tag(name = "Leech", description = "파워 리치 결정 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms/{roomId}/leech")
public class LeechController {

    private final PowerLeechService powerLeechService;

    @Operation(summary = "파워 리치 결정 (수락/거절)")
    @PostMapping("/{leechId}/decide")
    public ResponseEntity<Map<String, Object>> decideLeech(
            @PathVariable UUID roomId,
            @PathVariable UUID leechId,
            @RequestBody LeechDecideRequest request) {
        try {
            powerLeechService.decidePowerLeech(roomId, leechId, request.playerId(),
                    request.accept(), request.taklonsChoice());
            return ResponseEntity.ok(Map.of("success", true));
        } catch (Exception e) {
            return ResponseEntity.ok(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @Operation(summary = "현재 대기 중인 리치 오퍼 조회 (페이지 복구용)")
    @GetMapping("/pending")
    public ResponseEntity<List<GameLeechOffer>> getPendingLeeches(@PathVariable UUID roomId) {
        return ResponseEntity.ok(powerLeechService.getPendingOffers(roomId));
    }
}
