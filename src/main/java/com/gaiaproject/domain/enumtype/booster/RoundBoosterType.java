package com.gaiaproject.domain.enumtype.booster;

import com.gaiaproject.domain.enumtype.artifact.ArtifactType;
import com.gaiaproject.dto.PassContextVo;
import com.gaiaproject.dto.PassScoringVo;
import com.gaiaproject.dto.ResourcesVo;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * 라운드 부스터 타입
 * - 매 라운드마다 수입(income) 제공
 * - 패스 시 점수(passScoring) 계산
 * - 총 10개 (기본 게임) + 확장팩 추가
 */
@Getter
public enum RoundBoosterType {

    // ========== 기본 수입만 (패스 점수 없음) ==========

    /** 수입: 광석 1, 지식 1 */
    BOOSTER_1(
            new ResourcesVo(0, 1, 1, 0, 0, 0, 0, 0, 0, null),
            PassScoringVo.none(),
            BoosterActionType.NONE
    ),

    /** 수입: 크레딧 2, QIC 1 */
    BOOSTER_2(
            new ResourcesVo(2, 0, 0, 1, 0, 0, 0, 0, 0, null),
            PassScoringVo.none(),
            BoosterActionType.NONE
    ),

    /** 수입: 광석 1, 파워 토큰 2 */
    BOOSTER_3(
            new ResourcesVo(0, 1, 0, 0, 2, 0, 0, 0, 0, null),
            PassScoringVo.none(),
            BoosterActionType.NONE
    ),

    // ========== 수입 + 패스 점수 ==========

    /** 수입: 광석 1 / 패스: 광산 1개당 1VP */
    BOOSTER_4(
            new ResourcesVo(0, 1, 0, 0, 0, 0, 0, 0, 0, null),
            PassScoringVo.perMine(1),
            BoosterActionType.NONE
    ),

    /** 수입: 지식 1 / 패스: 연구소 1개당 3VP */
    BOOSTER_5(
            new ResourcesVo(0, 0, 1, 0, 0, 0, 0, 0, 0, null),
            PassScoringVo.perResearchLab(3),
            BoosterActionType.NONE
    ),

    /** 수입: 광석 1 / 패스: 교역소 1개당 2VP */
    BOOSTER_6(
            new ResourcesVo(0, 1, 0, 0, 0, 0, 0, 0, 0, null),
            PassScoringVo.perTradingStation(2),
            BoosterActionType.NONE
    ),

    /** 수입: 파워 순환 4 / 패스: 행성의회 + 아카데미 1개당 4VP */
    BOOSTER_7(
            new ResourcesVo(0, 0, 0, 0, 0, 0, 0, 4, 0, null),
            PassScoringVo.perPiOrAcademy(4),
            BoosterActionType.NONE
    ),

    /** 수입: 크레딧 4 / 패스: 가이아 행성 1개당 1VP */
    BOOSTER_8(
            new ResourcesVo(4, 0, 0, 0, 0, 0, 0, 0, 0, null),
            PassScoringVo.perGaiaPlanet(1),
            BoosterActionType.NONE
    ),

    /** 수입: 광석 1 / 패스: 행성 종류 1개당 1VP */
    BOOSTER_9(
            new ResourcesVo(0, 1, 0, 0, 0, 0, 0, 0, 0, null),
            PassScoringVo.perPlanetTypeKind(1),
            BoosterActionType.NONE
    ),

    /** 수입: 광석 1 / 패스: 가이아포머 1개당 3VP */
    BOOSTER_10(
            new ResourcesVo(0, 1, 0, 0, 0, 0, 0, 0, 0, null),
            PassScoringVo.perGaiaformer(3),
            BoosterActionType.NONE
    ),

    /** 수입: 크레딧 3 / 패스: 깊은 구역 건물 1개당 2VP */
    BOOSTER_11(
            new ResourcesVo(3, 0, 0, 0, 0, 0, 0, 0, 0, null),
            PassScoringVo.perDeepSectorStructure(2),
            BoosterActionType.NONE
    ),

    // ========== 수입 + 액션 ==========


    /** 수입: 파워 차징 2, 즉시 포밍 */
    BOOSTER_12(
            new ResourcesVo(0, 0, 0, 0, 0, 0, 0, 2, 0, null),
            PassScoringVo.none(),
            BoosterActionType.PLACE_GAIAFORMER
    ),

    /** 수입: 파워 차징 2, 항해 3 */
    BOOSTER_13(
            new ResourcesVo(0, 0, 0, 0, 0, 0, 0, 2, 0, null),
            PassScoringVo.none(),
            BoosterActionType.NAVIGATION_PLUS_3
    ),

    /** 수입: 크레딧 2, 테라포밍 1 */
    BOOSTER_14(
            new ResourcesVo(2, 0, 0, 0, 0, 0, 0, 0, 0, null),
            PassScoringVo.none(),
            BoosterActionType.TERRAFORM_ONE_STEP
    );

    private final ResourcesVo income;         // 매 라운드 수입
    private final PassScoringVo passScoring;  // 패스 시 점수 계산
    private final BoosterActionType actionType;  // 액션 타입

    RoundBoosterType(ResourcesVo income, PassScoringVo passScoring, BoosterActionType actionType) {
        this.income = income;
        this.passScoring = passScoring;
        this.actionType = actionType;
    }

    /**
     * 라운드 수입 반환
     */
    public ResourcesVo income() {
        return income;
    }

    /**
     * 패스 시 점수 계산
     * @param ctx 플레이어의 현재 게임 상태
     * @return 획득 VP
     */
    public int scoreOnPass(PassContextVo ctx) {
        return passScoring.score(ctx);
    }

    /**
     * 액션 존재 여부
     */
    public boolean hasAction() {
        return actionType != BoosterActionType.NONE;
    }

    /**
     * 라운드 부스터 14개 중 7개 랜덤 선택
     */
    public static List<RoundBoosterType> getRandomTiles(int count) {
        List<RoundBoosterType> all = new ArrayList<>(Arrays.asList(values()));
        Collections.shuffle(all);
        return all.subList(0, count);
    }
}