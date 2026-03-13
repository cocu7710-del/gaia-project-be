package com.gaiaproject.domain.enumtype.map;

import lombok.Getter;

import java.util.*;

/**
 * 섹터 타입 (헥스 데이터 포함)
 */
@Getter
public enum SectorType {

    SECTOR_1(1, false, """
            [
              {"q": 0, "r": -2, "planet": "EMPTY"},
              {"q": -1, "r": -1, "planet": "EMPTY"},
              {"q": 0, "r": -1, "planet": "EMPTY"},
              {"q": 1, "r": -2, "planet": "EMPTY"},
              {"q": -2, "r": 0, "planet": "EMPTY"},
              {"q": -1, "r": 0, "planet": "SWAMP"},
              {"q": 0, "r": 0, "planet": "EMPTY"},
              {"q": 1, "r": -1, "planet": "TERRA"},
              {"q": 2, "r": -2, "planet": "EMPTY"},
              {"q": -2, "r": 1, "planet": "DESERT"},
              {"q": -1, "r": 1, "planet": "EMPTY"},
              {"q": 0, "r": 1, "planet": "EMPTY"},
              {"q": 1, "r": 0, "planet": "EMPTY"},
              {"q": 2, "r": -1, "planet": "TRANSDIM"},
              {"q": -2, "r": 2, "planet": "EMPTY"},
              {"q": -1, "r": 2, "planet": "EMPTY"},
              {"q": 0, "r": 2, "planet": "VOLCANIC"},
              {"q": 1, "r": 1, "planet": "OXIDE"},
              {"q": 2, "r": 0, "planet": "EMPTY"}
            ]
            """),

    SECTOR_2(2, false, """
            [
              {"q": 0, "r": -2, "planet": "TITANIUM"},
              {"q": -1, "r": -1, "planet": "OXIDE"},
              {"q": 0, "r": -1, "planet": "EMPTY"},
              {"q": 1, "r": -2, "planet": "EMPTY"},
              {"q": -2, "r": 0, "planet": "EMPTY"},
              {"q": -1, "r": 0, "planet": "EMPTY"},
              {"q": 0, "r": 0, "planet": "EMPTY"},
              {"q": 1, "r": -1, "planet": "ICE"},
              {"q": 2, "r": -2, "planet": "EMPTY"},
              {"q": -2, "r": 1, "planet": "EMPTY"},
              {"q": -1, "r": 1, "planet": "SWAMP"},
              {"q": 0, "r": 1, "planet": "EMPTY"},
              {"q": 1, "r": 0, "planet": "EMPTY"},
              {"q": 2, "r": -1, "planet": "DESERT"},
              {"q": -2, "r": 2, "planet": "EMPTY"},
              {"q": -1, "r": 2, "planet": "VOLCANIC"},
              {"q": 0, "r": 2, "planet": "EMPTY"},
              {"q": 1, "r": 1, "planet": "TRANSDIM"},
              {"q": 2, "r": 0, "planet": "EMPTY"}
            ]
            """),

    SECTOR_3(3, false, """
            [
              {"q": 0, "r": -2, "planet": "TRANSDIM"},
              {"q": -1, "r": -1, "planet": "EMPTY"},
              {"q": 0, "r": -1, "planet": "EMPTY"},
              {"q": 1, "r": -2, "planet": "EMPTY"},
              {"q": -2, "r": 0, "planet": "EMPTY"},
              {"q": -1, "r": 0, "planet": "GAIA"},
              {"q": 0, "r": 0, "planet": "EMPTY"},
              {"q": 1, "r": -1, "planet": "EMPTY"},
              {"q": 2, "r": -2, "planet": "EMPTY"},
              {"q": -2, "r": 1, "planet": "EMPTY"},
              {"q": -1, "r": 1, "planet": "EMPTY"},
              {"q": 0, "r": 1, "planet": "EMPTY"},
              {"q": 1, "r": 0, "planet": "ICE"},
              {"q": 2, "r": -1, "planet": "TITANIUM"},
              {"q": -2, "r": 2, "planet": "EMPTY"},
              {"q": -1, "r": 2, "planet": "TERRA"},
              {"q": 0, "r": 2, "planet": "DESERT"},
              {"q": 1, "r": 1, "planet": "EMPTY"},
              {"q": 2, "r": 0, "planet": "EMPTY"}
            ]
            """),
    SECTOR_4(4, false, """
            [
              {"q": 0, "r": -2, "planet": "TITANIUM"},
              {"q": -1, "r": -1, "planet": "EMPTY"},
              {"q": 0, "r": -1, "planet": "VOLCANIC"},
              {"q": 1, "r": -2, "planet": "EMPTY"},
              {"q": -2, "r": 0, "planet": "EMPTY"},
              {"q": -1, "r": 0, "planet": "EMPTY"},
              {"q": 0, "r": 0, "planet": "EMPTY"},
              {"q": 1, "r": -1, "planet": "EMPTY"},
              {"q": 2, "r": -2, "planet": "EMPTY"},
              {"q": -2, "r": 1, "planet": "ICE"},
              {"q": -1, "r": 1, "planet": "OXIDE"},
              {"q": 0, "r": 1, "planet": "EMPTY"},
              {"q": 1, "r": 0, "planet": "SWAMP"},
              {"q": 2, "r": -1, "planet": "EMPTY"},
              {"q": -2, "r": 2, "planet": "EMPTY"},
              {"q": -1, "r": 2, "planet": "EMPTY"},
              {"q": 0, "r": 2, "planet": "EMPTY"},
              {"q": 1, "r": 1, "planet": "EMPTY"},
              {"q": 2, "r": 0, "planet": "TERRA"}
            ]
            """),

    SECTOR_5(5, false, """
            [
              {"q": 0, "r": -2, "planet": "ICE"},
              {"q": -1, "r": -1, "planet": "EMPTY"},
              {"q": 0, "r": -1, "planet": "EMPTY"},
              {"q": 1, "r": -2, "planet": "EMPTY"},
              {"q": -2, "r": 0, "planet": "EMPTY"},
              {"q": -1, "r": 0, "planet": "GAIA"},
              {"q": 0, "r": 0, "planet": "EMPTY"},
              {"q": 1, "r": -1, "planet": "EMPTY"},
              {"q": 2, "r": -2, "planet": "TRANSDIM"},
              {"q": -2, "r": 1, "planet": "EMPTY"},
              {"q": -1, "r": 1, "planet": "EMPTY"},
              {"q": 0, "r": 1, "planet": "EMPTY"},
              {"q": 1, "r": 0, "planet": "EMPTY"},
              {"q": 2, "r": -1, "planet": "VOLCANIC"},
              {"q": -2, "r": 2, "planet": "EMPTY"},
              {"q": -1, "r": 2, "planet": "OXIDE"},
              {"q": 0, "r": 2, "planet": "DESERT"},
              {"q": 1, "r": 1, "planet": "EMPTY"},
              {"q": 2, "r": 0, "planet": "EMPTY"}
            ]
            """),

    SECTOR_6(6, false, """
            [
              {"q": 0, "r": -2, "planet": "EMPTY"},
              {"q": -1, "r": -1, "planet": "EMPTY"},
              {"q": 0, "r": -1, "planet": "EMPTY"},
              {"q": 1, "r": -2, "planet": "TRANSDIM"},
              {"q": -2, "r": 0, "planet": "EMPTY"},
              {"q": -1, "r": 0, "planet": "SWAMP"},
              {"q": 0, "r": 0, "planet": "EMPTY"},
              {"q": 1, "r": -1, "planet": "TERRA"},
              {"q": 2, "r": -2, "planet": "EMPTY"},
              {"q": -2, "r": 1, "planet": "EMPTY"},
              {"q": -1, "r": 1, "planet": "EMPTY"},
              {"q": 0, "r": 1, "planet": "GAIA"},
              {"q": 1, "r": 0, "planet": "EMPTY"},
              {"q": 2, "r": -1, "planet": "EMPTY"},
              {"q": -2, "r": 2, "planet": "EMPTY"},
              {"q": -1, "r": 2, "planet": "EMPTY"},
              {"q": 0, "r": 2, "planet": "EMPTY"},
              {"q": 1, "r": 1, "planet": "TRANSDIM"},
              {"q": 2, "r": 0, "planet": "DESERT"}
            ]
            """),

    SECTOR_7(7, false, """
            [
              {"q": 0, "r": -2, "planet": "EMPTY"},
              {"q": -1, "r": -1, "planet": "EMPTY"},
              {"q": 0, "r": -1, "planet": "VOLCANIC"},
              {"q": 1, "r": -2, "planet": "SWAMP"},
              {"q": -2, "r": 0, "planet": "TRANSDIM"},
              {"q": -1, "r": 0, "planet": "EMPTY"},
              {"q": 0, "r": 0, "planet": "EMPTY"},
              {"q": 1, "r": -1, "planet": "EMPTY"},
              {"q": 2, "r": -2, "planet": "EMPTY"},
              {"q": -2, "r": 1, "planet": "EMPTY"},
              {"q": -1, "r": 1, "planet": "GAIA"},
              {"q": 0, "r": 1, "planet": "EMPTY"},
              {"q": 1, "r": 0, "planet": "GAIA"},
              {"q": 2, "r": -1, "planet": "EMPTY"},
              {"q": -2, "r": 2, "planet": "EMPTY"},
              {"q": -1, "r": 2, "planet": "EMPTY"},
              {"q": 0, "r": 2, "planet": "TITANIUM"},
              {"q": 1, "r": 1, "planet": "EMPTY"},
              {"q": 2, "r": 0, "planet": "EMPTY"}
            ]
            """),

    SECTOR_8(8, false, """
            [
              {"q": 0, "r": -2, "planet": "TERRA"},
              {"q": -1, "r": -1, "planet": "EMPTY"},
              {"q": 0, "r": -1, "planet": "ICE"},
              {"q": 1, "r": -2, "planet": "EMPTY"},
              {"q": -2, "r": 0, "planet": "EMPTY"},
              {"q": -1, "r": 0, "planet": "EMPTY"},
              {"q": 0, "r": 0, "planet": "EMPTY"},
              {"q": 1, "r": -1, "planet": "EMPTY"},
              {"q": 2, "r": -2, "planet": "TRANSDIM"},
              {"q": -2, "r": 1, "planet": "EMPTY"},
              {"q": -1, "r": 1, "planet": "OXIDE"},
              {"q": 0, "r": 1, "planet": "EMPTY"},
              {"q": 1, "r": 0, "planet": "TITANIUM"},
              {"q": 2, "r": -1, "planet": "EMPTY"},
              {"q": -2, "r": 2, "planet": "EMPTY"},
              {"q": -1, "r": 2, "planet": "TRANSDIM"},
              {"q": 0, "r": 2, "planet": "EMPTY"},
              {"q": 1, "r": 1, "planet": "EMPTY"},
              {"q": 2, "r": 0, "planet": "EMPTY"}
            ]
            """),

    SECTOR_9(9, false, """
            [
              {"q": 0, "r": -2, "planet": "EMPTY"},
              {"q": -1, "r": -1, "planet": "OXIDE"},
              {"q": 0, "r": -1, "planet": "EMPTY"},
              {"q": 1, "r": -2, "planet": "TRANSDIM"},
              {"q": -2, "r": 0, "planet": "EMPTY"},
              {"q": -1, "r": 0, "planet": "EMPTY"},
              {"q": 0, "r": 0, "planet": "EMPTY"},
              {"q": 1, "r": -1, "planet": "EMPTY"},
              {"q": 2, "r": -2, "planet": "ICE"},
              {"q": -2, "r": 1, "planet": "EMPTY"},
              {"q": -1, "r": 1, "planet": "TITANIUM"},
              {"q": 0, "r": 1, "planet": "EMPTY"},
              {"q": 1, "r": 0, "planet": "GAIA"},
              {"q": 2, "r": -1, "planet": "EMPTY"},
              {"q": -2, "r": 2, "planet": "SWAMP"},
              {"q": -1, "r": 2, "planet": "EMPTY"},
              {"q": 0, "r": 2, "planet": "EMPTY"},
              {"q": 1, "r": 1, "planet": "EMPTY"},
              {"q": 2, "r": 0, "planet": "EMPTY"}
            ]
            """),

    SECTOR_10(10, false, """
            [
              {"q": 0, "r": -2, "planet": "EMPTY"},
              {"q": -1, "r": -1, "planet": "EMPTY"},
              {"q": 0, "r": -1, "planet": "EMPTY"},
              {"q": 1, "r": -2, "planet": "TRANSDIM"},
              {"q": -2, "r": 0, "planet": "EMPTY"},
              {"q": -1, "r": 0, "planet": "DESERT"},
              {"q": 0, "r": 0, "planet": "EMPTY"},
              {"q": 1, "r": -1, "planet": "EMPTY"},
              {"q": 2, "r": -2, "planet": "TRANSDIM"},
              {"q": -2, "r": 1, "planet": "EMPTY"},
              {"q": -1, "r": 1, "planet": "EMPTY"},
              {"q": 0, "r": 1, "planet": "EMPTY"},
              {"q": 1, "r": 0, "planet": "GAIA"},
              {"q": 2, "r": -1, "planet": "EMPTY"},
              {"q": -2, "r": 2, "planet": "TERRA"},
              {"q": -1, "r": 2, "planet": "VOLCANIC"},
              {"q": 0, "r": 2, "planet": "EMPTY"},
              {"q": 1, "r": 1, "planet": "EMPTY"},
              {"q": 2, "r": 0, "planet": "EMPTY"}
            ]
            """),

    // Deep Sectors (1~8, Front/Back) - 프론트엔드 TRI_OFFSETS_BASE와 일치: (0,0), (1,0), (0,1)
    DEEP_SECTOR_1_FRONT(11, true, """
            [
              {"q": 0, "r": 0, "planet": "LOST_PLANET"},
              {"q": 1, "r": 0, "planet": "EMPTY"},
              {"q": 0, "r": 1, "planet": "ASTEROIDS"}
            ]
            """),

    DEEP_SECTOR_1_BACK(11, true, """
            [
              {"q": 0, "r": 0, "planet": "EMPTY"},
              {"q": 1, "r": 0, "planet": "EMPTY"},
              {"q": 0, "r": 1, "planet": "ASTEROIDS"}
            ]
            """),

    DEEP_SECTOR_2_FRONT(12, true, """
            [
              {"q": 0, "r": 0, "planet": "TRANSDIM"},
              {"q": 1, "r": 0, "planet": "EMPTY"},
              {"q": 0, "r": 1, "planet": "LOST_PLANET"}
            ]
            """),

    DEEP_SECTOR_2_BACK(12, true, """
            [
              {"q": 0, "r": 0, "planet": "ASTEROIDS"},
              {"q": 1, "r": 0, "planet": "EMPTY"},
              {"q": 0, "r": 1, "planet": "EMPTY"}
            ]
            """),

    DEEP_SECTOR_3_FRONT(13, true, """
            [
              {"q": 0, "r": 0, "planet": "TRANSDIM"},
              {"q": 1, "r": 0, "planet": "ASTEROIDS"},
              {"q": 0, "r": 1, "planet": "EMPTY"}
            ]
            """),

    DEEP_SECTOR_3_BACK(13, true, """
            [
              {"q": 0, "r": 0, "planet": "EMPTY"},
              {"q": 1, "r": 0, "planet": "ASTEROIDS"},
              {"q": 0, "r": 1, "planet": "EMPTY"}
            ]
            """),

    DEEP_SECTOR_4_FRONT(14, true, """
            [
              {"q": 0, "r": 0, "planet": "LOST_PLANET"},
              {"q": 1, "r": 0, "planet": "ASTEROIDS"},
              {"q": 0, "r": 1, "planet": "EMPTY"}
            ]
            """),

    DEEP_SECTOR_4_BACK(14, true, """
            [
              {"q": 0, "r": 0, "planet": "EMPTY"},
              {"q": 1, "r": 0, "planet": "ASTEROIDS"},
              {"q": 0, "r": 1, "planet": "EMPTY"}
            ]
            """),

    DEEP_SECTOR_5_FRONT(15, true, """
            [
              {"q": 0, "r": 0, "planet": "LOST_PLANET"},
              {"q": 1, "r": 0, "planet": "EMPTY"},
              {"q": 0, "r": 1, "planet": "EMPTY"}
            ]
            """),

    DEEP_SECTOR_5_BACK(15, true, """
            [
              {"q": 0, "r": 0, "planet": "LOST_PLANET"},
              {"q": 1, "r": 0, "planet": "ASTEROIDS"},
              {"q": 0, "r": 1, "planet": "EMPTY"}
            ]
            """),

    DEEP_SECTOR_6_FRONT(16, true, """
            [
              {"q": 0, "r": 0, "planet": "EMPTY"},
              {"q": 1, "r": 0, "planet": "LOST_PLANET"},
              {"q": 0, "r": 1, "planet": "EMPTY"}
            ]
            """),

    DEEP_SECTOR_6_BACK(16, true, """
            [
              {"q": 0, "r": 0, "planet": "ASTEROIDS"},
              {"q": 1, "r": 0, "planet": "ASTEROIDS"},
              {"q": 0, "r": 1, "planet": "EMPTY"}
            ]
            """),

    DEEP_SECTOR_7_FRONT(17, true, """
            [
              {"q": 0, "r": 0, "planet": "TRANSDIM"},
              {"q": 1, "r": 0, "planet": "EMPTY"},
              {"q": 0, "r": 1, "planet": "EMPTY"}
            ]
            """),

    DEEP_SECTOR_7_BACK(17, true, """
            [
              {"q": 0, "r": 0, "planet": "EMPTY"},
              {"q": 1, "r": 0, "planet": "EMPTY"},
              {"q": 0, "r": 1, "planet": "ASTEROIDS"}
            ]
            """),

    DEEP_SECTOR_8_FRONT(18, true, """
            [
              {"q": 0, "r": 0, "planet": "LOST_PLANET"},
              {"q": 1, "r": 0, "planet": "EMPTY"},
              {"q": 0, "r": 1, "planet": "EMPTY"}
            ]
            """),

    DEEP_SECTOR_8_BACK(18, true, """
            [
              {"q": 0, "r": 0, "planet": "EMPTY"},
              {"q": 1, "r": 0, "planet": "EMPTY"},
              {"q": 0, "r": 1, "planet": "ASTEROIDS"}
            ]
            """);

    private final int sectorNumber;
    private final boolean isDeepSector;
    private final String hexesJson;  // JSON 문자열

    SectorType(int sectorNumber, boolean isDeepSector, String hexesJson) {
        this.sectorNumber = sectorNumber;
        this.isDeepSector = isDeepSector;
        this.hexesJson = hexesJson;
    }

    /**
     * 기본 섹터만
     */
    public static List<SectorType> getBasicSectors() {
        return Arrays.stream(values())
                .filter(s -> !s.isDeepSector)
                .toList();
    }

    /**
     * 기본 섹터 10개 랜덤 순서
     */
    public static List<SectorType> getRandomBasicOrder() {
        List<SectorType> sectors = new ArrayList<>(getBasicSectors());
        Collections.shuffle(sectors);
        return sectors;
    }

    /**
     * 깊은 구역 (앞면만)
     */
    public static List<SectorType> getDeepSectorsFront() {
        return Arrays.stream(values())
                .filter(s -> s.isDeepSector && s.name().endsWith("_FRONT"))
                .toList();
    }

    /**
     * 깊은 구역 N개 랜덤 (앞/뒤면 포함)
     */
    public static List<SectorType> getRandomDeepSectors(int count) {
        List<SectorType> fronts = new ArrayList<>(getDeepSectorsFront());
        Collections.shuffle(fronts);

        List<SectorType> result = new ArrayList<>();
        for (int i = 0; i < Math.min(count, fronts.size()); i++) {
            // 앞면/뒷면 랜덤 선택
            SectorType front = fronts.get(i);
            boolean useFront = new Random().nextBoolean();

            if (useFront) {
                result.add(front);
            } else {
                // 뒷면 찾기
                String backName = front.name().replace("_FRONT", "_BACK");
                SectorType back = SectorType.valueOf(backName);
                result.add(back);
            }
        }

        return result;
    }

}