package com.gaiaproject.dto.response;

import java.util.List;
import java.util.Map;

/**
 * 라운드 및 최종 점수 타일 응답 DTO
 */
public record ScoringTilesResponse(
        List<RoundScoringInfo> roundScorings,
        List<FinalScoringInfo> finalScorings
) {
    /**
     * 라운드 점수 타일 정보
     */
    public record RoundScoringInfo(
            int roundNumber,
            String tileCode,
            String description
    ) {}

    /**
     * 최종 점수 타일 정보 (플레이어별 진행도 포함)
     */
    public record FinalScoringInfo(
            int position,
            String tileCode,
            String description,
            Map<String, Integer> playerProgress  // playerId → 현재 달성 개수
    ) {}
}
