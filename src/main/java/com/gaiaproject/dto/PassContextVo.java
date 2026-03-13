package com.gaiaproject.dto;

public record PassContextVo(
    int mines,
    int tradingStations,
    int researchLabs,
    int academies,            // 아카데미
    int planetaryInstitutes,  // 행성의회(PI)
    int gaiaPlanets,          // 식민지화한 가이아 행성 수
    int gaiaformers,          // 보유한 가이아포머 수
    int deepSectorStructures, // 깊은 구역 섹터의 구조물 수
    int colonizedPlanetTypeKinds // 식민지화한 행성 타입 "종류 수"
){}
