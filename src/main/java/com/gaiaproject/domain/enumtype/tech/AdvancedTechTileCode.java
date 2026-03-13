package com.gaiaproject.domain.enumtype.tech;

import com.gaiaproject.dto.TechAbility;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 고급 기술 타일 (Advanced Tech Tiles)
 * - 전체 풀에서 6개를 랜덤 선택
 * - 각 기술 트랙에 1개씩 배치
 */
@Getter
public enum AdvancedTechTileCode {

    // ========== IMMEDIATE (즉발) ==========

    ADV_TILE_1(
            TechAbility.builder()
                    .type(TechAbilityType.IMMEDIATE)
                    .specialEffect("VP_PER_SECTOR_BUILDING")
                    .description("즉발: 섹터 구역 건물당 2VP")
                    .build()
    ),

    ADV_TILE_2(
            TechAbility.builder()
                    .type(TechAbilityType.IMMEDIATE)
                    .specialEffect("ORE_PER_SECTOR_BUILDING")
                    .description("즉발: 섹터 구역 건물당 1광석")
                    .build()
    ),

    ADV_TILE_3(
            TechAbility.builder()
                    .type(TechAbilityType.IMMEDIATE)
                    .specialEffect("VP_PER_MINE")
                    .description("즉발: 광산 1개당 2VP")
                    .build()
    ),

    ADV_TILE_4(
            TechAbility.builder()
                    .type(TechAbilityType.IMMEDIATE)
                    .specialEffect("VP_PER_TRADING_STATION")
                    .description("즉발: 교역소 1개당 4VP")
                    .build()
    ),

    ADV_TILE_5(
            TechAbility.builder()
                    .type(TechAbilityType.IMMEDIATE)
                    .specialEffect("VP_PER_FEDERATION_TOKEN")
                    .description("즉발: 연방 토큰 1개당 5VP")
                    .build()
    ),

    ADV_TILE_6(
            TechAbility.builder()
                    .type(TechAbilityType.IMMEDIATE)
                    .specialEffect("VP_PER_GAIA_PLANET")
                    .description("즉발: 가이아 땅 1개당 2VP")
                    .build()
    ),

    // ========== ACTION (액션) ==========

    ADV_TILE_7(
            TechAbility.builder()
                    .type(TechAbilityType.ACTION)
                    .specialEffect("ACTION_ORE_3")
                    .description("액션: 광석 3")
                    .build()
    ),

    ADV_TILE_8(
            TechAbility.builder()
                    .type(TechAbilityType.ACTION)
                    .specialEffect("ACTION_KNOWLEDGE_3")
                    .description("액션: 지식 3")
                    .build()
    ),

    ADV_TILE_9(
            TechAbility.builder()
                    .type(TechAbilityType.ACTION)
                    .specialEffect("ACTION_QIC_1_CREDIT_5")
                    .description("액션: QIC 1, 크레딧 5")
                    .build()
    ),

    // ========== PASSIVE (패시브) ==========

    ADV_TILE_10(
            TechAbility.builder()
                    .type(TechAbilityType.PASSIVE)
                    .specialEffect("VP_PER_TERRAFORMING_STEP")
                    .description("패시브: 테라포밍 1당 2VP")
                    .build()
    ),

    ADV_TILE_11(
            TechAbility.builder()
                    .type(TechAbilityType.PASSIVE)
                    .specialEffect("VP_PER_LOST_PLANET_PASS")
                    .description("패시브: 패스 후 깊은 구역 타일 1개당 2VP")
                    .build()
    ),

    ADV_TILE_12(
            TechAbility.builder()
                    .type(TechAbilityType.PASSIVE)
                    .specialEffect("VP_PER_ASTEROID_SECTOR_PASS")
                    .description("패시브: 패스 후 소행성 구역당 2VP")
                    .build()
    ),

    ADV_TILE_13(
            TechAbility.builder()
                    .type(TechAbilityType.PASSIVE)
                    .specialEffect("VP_PER_FEDERATION_TOKEN_PASS")
                    .description("패시브: 패스 후 연방 토큰 1개당 3VP")
                    .build()
    ),

    ADV_TILE_14(
            TechAbility.builder()
                    .type(TechAbilityType.PASSIVE)
                    .specialEffect("VP_PER_LAB_PASS")
                    .description("패시브: 패스 후 연구소 1개당 3VP")
                    .build()
    ),

    ADV_TILE_15(
            TechAbility.builder()
                    .type(TechAbilityType.PASSIVE)
                    .specialEffect("VP_PER_PLANET_TYPE_PASS")
                    .description("패시브: 패스 후 행성 종류 1개당 1VP")
                    .build()
    ),

    ADV_TILE_16(
            TechAbility.builder()
                    .type(TechAbilityType.PASSIVE)
                    .specialEffect("VP_ON_MINE_BUILD")
                    .description("패시브: 광산 건설 시 3VP")
                    .build()
    ),

    ADV_TILE_17(
            TechAbility.builder()
                    .type(TechAbilityType.PASSIVE)
                    .specialEffect("VP_ON_TRADING_STATION_BUILD")
                    .description("패시브: 교역소 건설 시 3VP")
                    .build()
    ),

    ADV_TILE_18(
            TechAbility.builder()
                    .type(TechAbilityType.PASSIVE)
                    .specialEffect("VP_PER_KNOWLEDGE_TRACK_LEVEL")
                    .description("패시브: 지식 트랙 1칸당 2VP")
                    .build()
    ),

    ADV_TILE_19(
            TechAbility.builder()
                    .type(TechAbilityType.IMMEDIATE)
                    .specialEffect("VP_BIG")
                    .description("즉시: 큰 건물 당 6VP")
                    .build()
    ),

    ADV_TILE_20(
            TechAbility.builder()
                    .type(TechAbilityType.IMMEDIATE)
                    .specialEffect("PER_LOST_PLANET_VP4")
                    .description("즉시: 깊은 구역 타일 1개당 4VP")
                    .build()
    ),

    ADV_TILE_21(
            TechAbility.builder()
                    .type(TechAbilityType.PASSIVE)
                    .specialEffect("QIC_ACTION")
                    .description("패시브: QIC 액션당 4VP")
                    .build()
    );

    private final TechAbility ability;

    AdvancedTechTileCode(TechAbility ability) {
        this.ability = ability;
    }

    /**
     * 고급 기술 타일 7개 랜덤 선택
     */
    public static List<AdvancedTechTileCode> getRandomTile() {
        List<AdvancedTechTileCode> all = new ArrayList<>(Arrays.asList(values()));
        Collections.shuffle(all);
        return all.subList(0, 7);
    }
}