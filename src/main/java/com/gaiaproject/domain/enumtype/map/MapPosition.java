package com.gaiaproject.domain.enumtype.map;

import lombok.Getter;

/**
 * 4인 맵 Position별 글로벌 좌표 Offset
 * - 기본 섹터: position 1~10
 * - Deep 섹터: position 11~18 (12시부터 시계방향)
 * - Single 헥스: position 1~10
 */
@Getter
public enum MapPosition {

    // ===== 기본 섹터 (1~10) - FE HexMapDummy.tsx 확정 좌표 =====
    SECTOR_POS_1(1, -4, -1, 0, PositionType.SECTOR),
    SECTOR_POS_2(2, 1, -5, 0, PositionType.SECTOR),
    SECTOR_POS_3(3, 6, -9, 0, PositionType.SECTOR),
    SECTOR_POS_4(4, -5, 4, 0, PositionType.SECTOR),
    SECTOR_POS_5(5, 0, 0, 0, PositionType.SECTOR),
    SECTOR_POS_6(6, 5, -4, 0, PositionType.SECTOR),
    SECTOR_POS_7(7, 10, -8, 0, PositionType.SECTOR),
    SECTOR_POS_8(8, -1, 5, 0, PositionType.SECTOR),
    SECTOR_POS_9(9, 4, 1, 0, PositionType.SECTOR),
    SECTOR_POS_10(10, 9, -3, 0, PositionType.SECTOR),

    // ===== Deep 섹터 (11~18) - 3헥스 타일, 회전값 포함 =====
    DEEP_POS_11(11, -2, -5, 60, PositionType.DEEP_SECTOR),   // rotation: 1 (60도)
    DEEP_POS_12(12, 3, -9, 60, PositionType.DEEP_SECTOR),    // rotation: 1
    DEEP_POS_13(13, 9, -11, 0, PositionType.DEEP_SECTOR),    // rotation: 0
    DEEP_POS_14(14, 12, -7, 60, PositionType.DEEP_SECTOR),   // rotation: 1
    DEEP_POS_15(15, 7, 0, 0, PositionType.DEEP_SECTOR),      // rotation: 0
    DEEP_POS_16(16, 2, 4, 0, PositionType.DEEP_SECTOR),      // rotation: 0
    DEEP_POS_17(17, -4, 6, 60, PositionType.DEEP_SECTOR),    // rotation: 1
    DEEP_POS_18(18, -7, 2, 0, PositionType.DEEP_SECTOR),     // rotation: 0

    // ===== Single 헥스 (21~30) - 섹터 사이 빈 공간 =====
    SINGLE_POS_1(21, -3, 1, 0, PositionType.SINGLE_HEX),
    SINGLE_POS_2(22, -1, -2, 0, PositionType.SINGLE_HEX),
    SINGLE_POS_3(23, 2, -3, 0, PositionType.SINGLE_HEX),
    SINGLE_POS_4(24, 4, -6, 0, PositionType.SINGLE_HEX),
    SINGLE_POS_5(25, 7, -7, 0, PositionType.SINGLE_HEX),
    SINGLE_POS_6(26, 8, -5, 0, PositionType.SINGLE_HEX),
    SINGLE_POS_7(27, 6, -2, 0, PositionType.SINGLE_HEX),
    SINGLE_POS_8(28, 3, -1, 0, PositionType.SINGLE_HEX),
    SINGLE_POS_9(29, 1, 2, 0, PositionType.SINGLE_HEX),
    SINGLE_POS_10(30, -2, 3, 0, PositionType.SINGLE_HEX);

    private final int positionNo;
    private final int offsetQ;
    private final int offsetR;
    private final int defaultRotation;
    private final PositionType type;

    MapPosition(int positionNo, int offsetQ, int offsetR, int defaultRotation, PositionType type) {
        this.positionNo = positionNo;
        this.offsetQ = offsetQ;
        this.offsetR = offsetR;
        this.defaultRotation = defaultRotation;
        this.type = type;
    }

    public int getDefaultRotation() {
        return defaultRotation;
    }

    public enum PositionType {
        SECTOR,
        DEEP_SECTOR,
        SINGLE_HEX
    }

    /**
     * 기본 섹터 position 조회
     */
    public static MapPosition getSectorPosition(int positionNo) {
        for (MapPosition pos : values()) {
            if (pos.type == PositionType.SECTOR && pos.positionNo == positionNo) {
                return pos;
            }
        }
        throw new IllegalArgumentException("Invalid sector position: " + positionNo);
    }

    /**
     * Deep 섹터 position 조회
     */
    public static MapPosition getDeepSectorPosition(int positionNo) {
        for (MapPosition pos : values()) {
            if (pos.type == PositionType.DEEP_SECTOR && pos.positionNo == positionNo) {
                return pos;
            }
        }
        throw new IllegalArgumentException("Invalid deep sector position: " + positionNo);
    }

    /**
     * Single 헥스 position 조회
     * @param positionNo 1~10 (내부적으로 21~30으로 변환)
     */
    public static MapPosition getSingleHexPosition(int positionNo) {
        int internalPosNo = positionNo + 20; // 1~10 → 21~30
        for (MapPosition pos : values()) {
            if (pos.type == PositionType.SINGLE_HEX && pos.positionNo == internalPosNo) {
                return pos;
            }
        }
        throw new IllegalArgumentException("Invalid single hex position: " + positionNo);
    }
}
