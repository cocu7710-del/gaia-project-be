package com.gaiaproject.domain.enumtype.building;

import lombok.Getter;

/**
 * 건물 타입
 */
@Getter
public enum BuildingType {
    MINE("광산", 1),
    TRADING_STATION("교역소", 2),
    RESEARCH_LAB("연구소", 3),
    PLANETARY_INSTITUTE("행성 의회", 4),
    ACADEMY("아카데미", 5),
    GAIAFORMER("가이아포머", 6);

    private final String displayName;
    private final int powerValue;  // 연방 형성 시 파워 값

    BuildingType(String displayName, int powerValue) {
        this.displayName = displayName;
        this.powerValue = powerValue;
    }
}
