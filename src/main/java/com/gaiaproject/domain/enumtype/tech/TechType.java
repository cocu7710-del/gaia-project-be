package com.gaiaproject.domain.enumtype.tech;

import lombok.Getter;

/**
 * 기술 타일 타입 정의
 * - BASIC: 기본 기술타입
 * - ADVANCED: 고급 기술타입
 */

@Getter
public enum TechType {
    // 기본 게임용 9장 예시 (너 실제 타일 개수/코드에 맞춰 수정)
    BASIC,
    ADVANCED,
}
