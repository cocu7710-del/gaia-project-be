package com.gaiaproject.dto.request;

import java.util.UUID;

public record PassRoundRequest(
        UUID playerId,
        String nextRoundBoosterCode  // 다음 라운드에 사용할 부스터 코드
) {
}
