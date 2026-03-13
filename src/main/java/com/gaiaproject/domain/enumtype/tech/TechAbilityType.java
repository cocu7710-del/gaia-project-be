package com.gaiaproject.domain.enumtype.tech;

/**
 * 기술 타일 능력 발동 시점
 */
public enum TechAbilityType {
    INCOME,      // 수입 (매 라운드)
    ACTION,      // 액션 (게임 중 사용)
    IMMEDIATE,   // 즉발 (획득 즉시)
    PASSIVE      // 패시브 (지속 효과)
}