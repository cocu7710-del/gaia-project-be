package com.gaiaproject.domain.enumtype.tech;

import lombok.Getter;

import java.util.Arrays;
import java.util.List;

/**
 * 지식 트랙 종류
 * - 각 트랙 위에 고급 기술 타일 1장씩 배치됨
 * - EXPANSION은 확장판 전용 슬롯
 */
@Getter
public enum TechCategoryType {
    TERRA_FORMING("테라포밍"),
    NAVIGATION("항해"),
    AI("인공지능"),
    GAIA_FORMING("가이아포밍"),
    ECONOMY("경제"),
    SCIENCE("과학"),
    COMMON("공용"),
    EXPANSION("확장");

    private final String displayName;

    TechCategoryType(String displayName) {
        this.displayName = displayName;
    }

    /**
     * 순서대로 모든 트랙 반환
     */
    public static List<TechCategoryType> getList() {
        return Arrays.asList(values());
    }

}