package com.gaiaproject.controller;

import com.gaiaproject.dto.request.PassRoundRequest;
import com.gaiaproject.dto.response.PassRoundResponse;
import com.gaiaproject.service.PassService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Slf4j
@Tag(name = "Pass", description = "패스 관련 API")
@RestController
@RequestMapping("/api/rooms/{roomId}")
@RequiredArgsConstructor
public class PassController {

    private final PassService passService;

    @Operation(summary = "라운드 패스 (다음 라운드 부스터 선택)")
    @PostMapping("/pass")
    public ResponseEntity<PassRoundResponse> passRound(
            @PathVariable UUID roomId,
            @RequestBody PassRoundRequest request
    ) {
        int burnCount = request.burnPowerCount() == null ? 0 : request.burnPowerCount();
        int freeCount = request.freeConverts() == null ? 0 : request.freeConverts().size();
        log.info("패스 요청: roomId={}, playerId={}, nextBooster={}, burn={}, freeConverts={}",
                roomId, request.playerId(), request.nextRoundBoosterCode(), burnCount, freeCount);
        PassRoundResponse response = passService.passRound(
                roomId, request.playerId(), request.nextRoundBoosterCode(),
                request.burnPowerCount(), request.freeConverts());
        return ResponseEntity.ok(response);
    }
}
