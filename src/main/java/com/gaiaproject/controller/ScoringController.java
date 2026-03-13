package com.gaiaproject.controller;

import com.gaiaproject.domain.entity.rounds.GameFinalScoring;
import com.gaiaproject.domain.entity.rounds.GameRoundScoring;
import com.gaiaproject.dto.response.ScoringTilesResponse;
import com.gaiaproject.dto.response.ScoringTilesResponse.FinalScoringInfo;
import com.gaiaproject.dto.response.ScoringTilesResponse.RoundScoringInfo;
import com.gaiaproject.repository.rounds.GameFinalScoringRepository;
import com.gaiaproject.repository.rounds.GameRoundScoringRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@Tag(name = "Scoring", description = "라운드 및 최종 점수 타일 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms/{roomId}/scoring")
public class ScoringController {

    private final GameRoundScoringRepository roundScoringRepository;
    private final GameFinalScoringRepository finalScoringRepository;

    @Operation(summary = "라운드 및 최종 점수 타일 조회")
    @GetMapping
    public ResponseEntity<ScoringTilesResponse> getScoringTiles(@PathVariable UUID roomId) {
        // 1. 라운드 점수 타일 조회
        List<GameRoundScoring> roundScorings = roundScoringRepository.findByGameIdOrderByRoundNumber(roomId);
        List<RoundScoringInfo> roundInfos = roundScorings.stream()
                .map(rs -> new RoundScoringInfo(
                        rs.getRoundNumber(),
                        rs.getScoringTileCode().name(),
                        rs.getScoringTileCode().getDescription()
                ))
                .toList();

        // 2. 최종 점수 타일 조회
        List<GameFinalScoring> finalScorings = finalScoringRepository.findByGameIdOrderByPosition(roomId);
        List<FinalScoringInfo> finalInfos = finalScorings.stream()
                .map(fs -> new FinalScoringInfo(
                        fs.getPosition(),
                        fs.getScoringTileCode().name(),
                        fs.getScoringTileCode().getDescription()
                ))
                .toList();

        return ResponseEntity.ok(new ScoringTilesResponse(roundInfos, finalInfos));
    }
}
