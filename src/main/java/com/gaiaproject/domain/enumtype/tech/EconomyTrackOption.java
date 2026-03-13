package com.gaiaproject.domain.enumtype.tech;

import java.util.Random;

/**
 * 경제 트랙 옵션 (확장판 변경)
 * - 게임 시작 시 랜덤 선택
 * - 레벨 3, 4의 수입이 옵션에 따라 다름
 */
public enum EconomyTrackOption {

    /**
     * 옵션 A: VP 보너스
     * - 레벨 3: 크레딧 3, 광석 1, VP 1
     * - 레벨 4: 크레딧 4, 광석 2, VP 1
     */
    OPTION_A,

    /**
     * 옵션 B: 파워 차징 보너스
     * - 레벨 3: 크레딧 2, 광석 1, 파워 차징 3
     * - 레벨 4: 크레딧 2, 광석 2, 파워 차징 2
     */
    OPTION_B;

    private static final Random RANDOM = new Random();

    /**
     * 랜덤으로 옵션 선택
     */
    public static EconomyTrackOption random() {
        return values()[RANDOM.nextInt(values().length)];
    }
}
