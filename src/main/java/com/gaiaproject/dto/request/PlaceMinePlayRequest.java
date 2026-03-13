package com.gaiaproject.dto.request;

import java.util.UUID;

public record PlaceMinePlayRequest(
        UUID playerId,
        int hexQ,
        int hexR,
        int qicUsed,            // 항법 거리 확장에 사용한 Qic 수
        boolean gaiaformerUsed, // 소행성(비홈) 건설 시 가이아포머 제거 여부
        int terraformDiscount   // 파워액션/부스터액션으로 받은 테라포밍 단계 할인
) {}
