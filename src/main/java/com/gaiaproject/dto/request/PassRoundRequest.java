package com.gaiaproject.dto.request;

import java.util.List;
import java.util.UUID;

/**
 * 라운드 패스 요청.
 *
 * Phase 3: 파워 소각 / 프리 자원 변환을 한 요청으로 통합.
 * FE 가 개별 API 를 순차 호출하던 것을 payload 에 포함하여 1 회 호출로 처리.
 */
public record PassRoundRequest(
        UUID playerId,
        String nextRoundBoosterCode,          // 다음 라운드에 사용할 부스터 코드 (R6 패스 시 null)
        Integer burnPowerCount,                // 패스 직전 수행할 파워 소각 횟수 (nullable → 0)
        List<FreeConvertEntry> freeConverts   // 패스 직전 수행할 자원 변환들 (nullable → 빈 리스트)
) {
    public record FreeConvertEntry(
            String convertCode,
            Boolean useBrainstone            // nullable → false
    ) {}
}
