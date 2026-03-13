package com.gaiaproject.domain.enumtype.federation;

/**
 * 연방 타일 특수 액션 타입
 */
public enum FederationActionType {
    NONE,                           // 액션 없음
    GAIN_BASIC_TECH_TILE,           // 기본 기술타일 1개 가져오기
    TERRAFORM_3_PLACE_MINE,         // 3 테라포밍 + 무료 광산
    PLACE_MINE_NO_RANGE_LIMIT,      // 사거리 제한 없이 무료 광산
    POWER_TOKEN_TO_BOWL_3           // 파워 토큰을 3구역에 추가
}