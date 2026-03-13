package com.gaiaproject.dto;

public record ResourcesVo(
        int credits,
        int ore,
        int knowledge,
        int qic,
        int powerBowl1,      // Bowl 1에 토큰 추가
        int powerBowl2,      // Bowl 2에 토큰 추가
        int powerBowl3,      // Bowl 3에 토큰 추가
        int powerCharge,     // 파워 차징 (Bowl 1→2→3 순환)
        int vp,
        Integer brainstoneBowl  // 타클론 전용 (null: 없음, 1/2/3: 해당 bowl)
) {
    public static ResourcesVo zero() {
        return new ResourcesVo(0, 0, 0, 0, 0, 0, 0, 0, 0, null);
    }

    public ResourcesVo add(ResourcesVo other) {
        return new ResourcesVo(
                this.credits + other.credits,
                this.ore + other.ore,
                this.knowledge + other.knowledge,
                this.qic + other.qic,
                this.powerBowl1 + other.powerBowl1,
                this.powerBowl2 + other.powerBowl2,
                this.powerBowl3 + other.powerBowl3,
                this.powerCharge + other.powerCharge,
                this.vp + other.vp,
                this.brainstoneBowl != null ? this.brainstoneBowl : other.brainstoneBowl
        );
    }
}