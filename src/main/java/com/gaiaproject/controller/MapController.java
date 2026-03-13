package com.gaiaproject.controller;

import com.gaiaproject.domain.entity.map.GameHex;
import com.gaiaproject.repository.map.GameHexRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Map", description = "맵 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms/{roomId}/map")
public class MapController {

    private final GameHexRepository gameHexRepository;

    @Operation(summary = "게임 맵 전체 헥스 조회")
    @GetMapping("/hexes")
    public ResponseEntity<List<GameHex>> getAllHexes(@PathVariable UUID roomId) {
        List<GameHex> hexes = gameHexRepository.findByGameId(roomId);
        return ResponseEntity.ok(hexes);
    }
}
