package com.gaiaproject.domain.enumtype.rounds;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 최종 점수 타일
 */
@Getter
public enum FinalScoringTileType {

    // 최종 점수 타일 (9개 중 2개 랜덤)
    /** (1위: 18VP, 2위: 12VP, 3위: 6VP) */
    FINAL_TILE_ASTEROID("소행성 개수"),
    FINAL_TILE_GAIA_PLANET("가이아 행성 개수"),
    FINAL_TILE_MOST_BUILDINGS("총 건물 개수"),
    FINAL_TILE_FEDERATION_BUILDINGS("연방 소속 건물 개수"),
    FINAL_TILE_DEEP_SECTORS("깊은 구역 섹터 개수"),
    FINAL_TILE_PLANET_TYPES("개척한 행성 종류 개수"),
    FINAL_TILE_FEDERATION_POWER("연방 시 받은 파워 토큰 총합"),
    FINAL_TILE_PI_ACADEMY_DISTANCE("의회-아카데미 거리"),
    FINAL_TILE_SECTORS_WITH_BUILDINGS("건물 1개 이상 섹터 개수");

    private final String description;

    FinalScoringTileType(String description) {
        this.description = description;
    }

    /**
     * 최종 점수용 2개 랜덤 선택
     */
    public static List<FinalScoringTileType> getRandomTwo() {
        List<FinalScoringTileType> all = new ArrayList<>(Arrays.asList(values()));
        Collections.shuffle(all);
        return all.subList(0, 2);
    }
}
