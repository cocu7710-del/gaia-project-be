package com.gaiaproject.domain.enumtype.tech;

import com.gaiaproject.dto.TechAbility;
import lombok.Getter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
public enum TechTileCode {

    // ========== 기본 게임 타일 (9장) ==========

    BASIC_TILE_1(
            false,
            TechAbility.builder()
                    .type(TechAbilityType.ACTION)
                    .specialEffect("CHARGE_POWER_4")
                    .description("액션: 파워 4 차징")
                    .build()
    ),

    BASIC_TILE_2(
            false,
            TechAbility.builder()
                    .type(TechAbilityType.INCOME)
                    .oreIncome(1)
                    .powerCharge(1)
                    .description("수입: 광석 1, 파워 차징 1")
                    .build()
    ),

    BASIC_TILE_3(
            false,
            TechAbility.builder()
                    .type(TechAbilityType.INCOME)
                    .creditIncome(4)
                    .description("수입: 크레딧 4")
                    .build()
    ),

    BASIC_TILE_4(
            false,
            TechAbility.builder()
                    .type(TechAbilityType.INCOME)
                    .knowledgeIncome(1)
                    .creditIncome(1)
                    .description("수입: 지식 1, 크레딧 1")
                    .build()
    ),

    BASIC_TILE_5(
            false,
            TechAbility.builder()
                    .type(TechAbilityType.IMMEDIATE)
                    .qicIncome(1)
                    .oreIncome(1)
                    .description("즉발: QIC 1, 광석 1")
                    .build()
    ),

    BASIC_TILE_6(
            false,
            TechAbility.builder()
                    .type(TechAbilityType.IMMEDIATE)
                    .vpGain(7)
                    .description("즉발: VP 7")
                    .build()
    ),

    BASIC_TILE_7(
            false,
            TechAbility.builder()
                    .type(TechAbilityType.IMMEDIATE)
                    .specialEffect("KNOWLEDGE_PER_PLANET_TYPE")
                    .description("즉발: 행성 유형당 지식 1")
                    .build()
    ),

    BASIC_TILE_8(
            false,
            TechAbility.builder()
                    .type(TechAbilityType.PASSIVE)
                    .specialEffect("GAIA_SECTOR_MINE_VP_3")
                    .description("패시브: 가이아 구역 광산 건설 시 VP +3")
                    .build()
    ),

    BASIC_TILE_9(
            false,
            TechAbility.builder()
                    .type(TechAbilityType.PASSIVE)
                    .specialEffect("BUILDING_UPGRADE_POWER_4")
                    .description("패시브: 건물 레벨 3→4 승급 시 파워 4 차징")
                    .build()
    ),

    // ========== 확장팩 타일 (3장) ==========

    BASIC_EXP_TILE_1(
            true,
            TechAbility.builder()
                    .type(TechAbilityType.PASSIVE)
                    .specialEffect("RANGE_PLUS_1")
                    .description("패시브: 항해 거리 +1")
                    .build()
    ),

    BASIC_EXP_TILE_2(
            true,
            TechAbility.builder()
                    .type(TechAbilityType.IMMEDIATE)
                    .oreIncome(1)
                    .knowledgeIncome(3)
                    .description("즉발: 광석 1, 지식 3")
                    .build()
    ),

    BASIC_EXP_TILE_3(
            true,
            TechAbility.builder()
                    .type(TechAbilityType.IMMEDIATE)
                    .specialEffect("TERRAFORM_2_PLACE_MINE")
                    .description("즉발: 테라포밍 2단계 후 광산 즉시 건설")
                    .build()
    );

    private final boolean isExpansion;
    private final TechAbility ability;

    TechTileCode(boolean isExpansion, TechAbility ability) {
        this.isExpansion = isExpansion;
        this.ability = ability;
    }

    /**
     * 기본 게임 타일 셔플 후 반환
     */
    public static List<TechTileCode> getBasicTiles() {
        List<TechTileCode> baseList = Arrays.stream(values())
                .filter(tile -> !tile.isExpansion)
                .collect(Collectors.toList());

        Collections.shuffle(baseList);

        return baseList;
    }

    /**
     * 확장팩 타일 셔플 후 반환
     */
    public static List<TechTileCode> getExpansionTiles() {
        List<TechTileCode> explist = Arrays.stream(values())
                .filter(tile -> tile.isExpansion)
                .collect(Collectors.toList());

        Collections.shuffle(explist);

        return explist;
    }

    /**
     * 모든 타일 반환 (기본 + 확장)
     */
    public static List<TechTileCode> getAllTiles() {
        return Arrays.asList(values());
    }
}