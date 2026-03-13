package com.gaiaproject.domain.enumtype.action;

/**
 * 파워 액션 타입 (key = FE 코드와 동일)
 */
public enum PowerActionType {

    /* 기본 파워 액션 */

    /** 파워 7 → 지식 3 */
    PWR_KNOWLEDGE,

    /** 파워 5 → 테라포밍 2단계 */
    PWR_TERRAFORM_2,

    /** 파워 4 → 광석 2 */
    PWR_ORE,

    /** 파워 4 → 크레딧 7 */
    PWR_CREDIT,

    /** 파워 4 → 지식 2 */
    PWR_KNOWLEDGE_2,

    /** 파워 3 → 테라포밍 1단계 */
    PWR_TERRAFORM,

    /** 파워 3 → 파워 토큰 +2 */
    PWR_TOKEN,


    // ========== 확장판 함대 파워 액션 ==========

    /* TF 마스 함대 */
    /** QIC 2 → 보유 기술 타일당 1VP + 2VP */
    FLEET_TF_MARS_1,

    /** 파워 2 → 즉시 가이아 포밍 | 조건: 가이아 포머 */
    FLEET_TF_MARS_2,

    /** 크레딧 3 → 테라포밍 1 */
    FLEET_TF_MARS_3,

    /* 이클립스 함대 */
    /** QIC 2 → 보유 행성 종류당 1VP + 2VP */
    FLEET_ECLIPSE_1,

    /** 파워 3, 지식 2 → 지식 트랙 전진 */
    FLEET_ECLIPSE_2,

    /** 크레딧 6 → 소행성에 무료 광산 */
    FLEET_ECLIPSE_3,

    /* 리벨리온 함대 */
    /** QIC 3 → 기본 기술 타일 가져오기 */
    FLEET_REBELLION_1,

    /** 파워 3, 광석 1 → 광산을 교역소로 업그레이드 */
    FLEET_REBELLION_2,

    /** 지식 2 → QIC 1, 크레딧 2 획득 */
    FLEET_REBELLION_3,

    /* 트와일라잇 함대 */
    /** QIC 3 → 보유한 연방 토큰 보상 1회 받기 */
    FLEET_TWILIGHT_1,

    /** 파워 3, 광석 2 → 교역소를 연구소로 업그레이드 */
    FLEET_TWILIGHT_2,

    /** 지식 1 → 즉시 +3 항해 거리 사용 */
    FLEET_TWILIGHT_3,
}
