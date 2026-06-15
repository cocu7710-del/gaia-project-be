package com.gaiaproject.controller;

import com.gaiaproject.domain.entity.building.GameBuilding;
import com.gaiaproject.domain.entity.game.GameSeat;
import com.gaiaproject.domain.entity.map.GameHex;
import com.gaiaproject.domain.entity.rounds.GameFinalScoring;
import com.gaiaproject.domain.entity.rounds.GameRoundScoring;
import com.gaiaproject.domain.enumtype.building.BuildingType;
import com.gaiaproject.domain.enumtype.player.PlanetType;
import com.gaiaproject.dto.response.ScoringTilesResponse;
import com.gaiaproject.dto.response.ScoringTilesResponse.FinalScoringInfo;
import com.gaiaproject.dto.response.ScoringTilesResponse.RoundScoringInfo;
import com.gaiaproject.service.GameCalculationService;
import com.gaiaproject.repository.building.GameBuildingRepository;
import com.gaiaproject.repository.game.GameSeatRepository;
import com.gaiaproject.repository.map.GameHexRepository;
import com.gaiaproject.repository.rounds.GameFinalScoringRepository;
import com.gaiaproject.repository.rounds.GameRoundScoringRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import java.util.*;
import java.util.stream.Collectors;

@Tag(name = "Scoring", description = "라운드 및 최종 점수 타일 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/rooms/{roomId}/scoring")
public class ScoringController {

    private final GameRoundScoringRepository roundScoringRepository;
    private final GameFinalScoringRepository finalScoringRepository;
    private final GameBuildingRepository buildingRepository;
    private final GameHexRepository hexRepository;
    private final GameSeatRepository seatRepository;
    private final com.gaiaproject.repository.federation.GameFederationGroupRepository federationGroupRepository;
    private final com.gaiaproject.repository.federation.GameFederationBuildingRepository federationBuildingRepository;
    private final com.gaiaproject.service.VpLogService vpLogService;
    private final com.gaiaproject.repository.player.GamePlayerStateRepository playerStateRepository;
    private final com.gaiaproject.repository.player.GamePlayerFederationTokenRepository playerFederationTokenRepository;
    private final com.gaiaproject.repository.federation.GameFederationTokenHexRepository federationTokenHexRepository;
    private final com.gaiaproject.repository.player.GamePlayerArtifactRepository playerArtifactRepository;
    private final GameCalculationService gameCalculationService;

    @Operation(summary = "게임 결과 조회 (카테고리별 VP)")
    @GetMapping("/result")
    public ResponseEntity<Map<String, Object>> getGameResult(@PathVariable UUID roomId) {
        var categoryScores = vpLogService.getGameResult(roomId);
        var perRoundScores = vpLogService.getDetailScores(roomId);
        var seats = seatRepository.findByGameIdOrderBySeatNoAsc(roomId);
        var allPlayerStates = playerStateRepository.findByGameId(roomId);

        List<Map<String, Object>> players = new ArrayList<>();
        for (var seat : seats) {
            if (seat.getPlayerId() == null) continue;
            Map<String, Object> playerResult = new LinkedHashMap<>();
            playerResult.put("playerId", seat.getPlayerId().toString());
            playerResult.put("seatNo", seat.getSeatNo());
            playerResult.put("factionCode", seat.getFactionType().name());
            playerResult.put("factionNameKo", seat.getFactionType().getDisplayNameKo());

            Map<String, Integer> cats = new LinkedHashMap<>();
            var playerCats = categoryScores.getOrDefault(seat.getPlayerId(), Map.of());
            for (var cat : com.gaiaproject.domain.enumtype.action.VpCategory.values()) {
                int val = playerCats.getOrDefault(cat, 0);
                cats.put(cat.name(), val);
            }
            playerResult.put("categoryScores", cats);
            // 라운드별 점수 (ROUND_SCORING_R1~R6, BOOSTER_PASS_R1~R6)
            playerResult.put("roundScores", perRoundScores.getOrDefault(seat.getPlayerId(), Map.of()));
            // totalVP는 실제 victoryPoints 필드에서 가져옴
            int totalVP = allPlayerStates.stream()
                    .filter(ps -> ps.getPlayerId().equals(seat.getPlayerId()))
                    .findFirst()
                    .map(ps -> ps.getVictoryPoints())
                    .orElse(0);
            playerResult.put("totalVP", totalVP);
            players.add(playerResult);
        }

        return ResponseEntity.ok(Map.of("players", players));
    }

    @Operation(summary = "라운드 및 최종 점수 타일 조회 (플레이어별 진행도 포함)")
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

        // 2. 최종 점수 타일 조회 + 플레이어별 진행도 계산
        List<GameFinalScoring> finalScorings = finalScoringRepository.findByGameIdOrderByPosition(roomId);
        List<GameSeat> seats = seatRepository.findByGameIdOrderBySeatNoAsc(roomId);
        List<UUID> playerIds = seats.stream()
                .map(GameSeat::getPlayerId)
                .filter(Objects::nonNull)
                .toList();

        // 건물, 헥스 데이터 미리 로드
        List<GameBuilding> allBuildings = buildingRepository.findByGameId(roomId);
        List<GameHex> allHexes = hexRepository.findByGameId(roomId);
        Map<String, GameHex> hexByCoord = allHexes.stream()
                .collect(Collectors.toMap(h -> h.getHexQ() + "," + h.getHexR(), h -> h, (a, b) -> a));

        List<FinalScoringInfo> finalInfos = finalScorings.stream()
                .map(fs -> {
                    Map<String, Integer> progress = new LinkedHashMap<>();
                    for (UUID pid : playerIds) {
                        int count = gameCalculationService.calcFinalProgress(roomId, fs.getScoringTileCode().name(), pid, allBuildings, hexByCoord);
                        progress.put(pid.toString(), count);
                    }
                    return new FinalScoringInfo(
                            fs.getPosition(),
                            fs.getScoringTileCode().name(),
                            fs.getScoringTileCode().getDescription(),
                            progress
                    );
                })
                .toList();

        return ResponseEntity.ok(new ScoringTilesResponse(roundInfos, finalInfos));
    }
}
