package com.gaiaproject.domain.enumtype.map;

import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 확장판 1헥스 타일 (랜덤 배치용)
 */
@Getter
public enum SingleHexTileType {
    // 트랜스딤 행성
    SINGLE_TRANSDIM_1(1, "LOST_PLANET"),

    // 소행성
    SINGLE_ASTEROIDS_1(2, "ASTEROIDS"),
    SINGLE_ASTEROIDS_2(3, "ASTEROIDS"),
    SINGLE_ASTEROIDS_3(4, "ASTEROIDS"),
    SINGLE_ASTEROIDS_4(5, "ASTEROIDS"),

    // 잊혀진 함대 우주선
    FORGOTTEN_FLEET_TF_MARS(6, "TF_MARS"),
    FORGOTTEN_FLEET_REBELLION(7, "REBELLION"),
    FORGOTTEN_FLEET_ECLIPSE(8, "ECLIPSE"),
    FORGOTTEN_FLEET_TWILIGHT(10, "TWILIGHT"),

    // 빈 공간
    SINGLE_EMPTY_1(9, "EMPTY");

    private final int tileNumber;
    private final String planetType;

    SingleHexTileType(int tileNumber, String planetType) {
        this.tileNumber = tileNumber;
        this.planetType = planetType;
    }

    /**
     * 모든 1헥스 타일 목록
     */
    public static List<SingleHexTileType> getAllTiles() {
        return List.of(values());
    }

    /**
     * 랜덤 순서로 섞은 타일 목록
     */
    public static List<SingleHexTileType> getShuffledTiles() {
        List<SingleHexTileType> tiles = new ArrayList<>(getAllTiles());
        Collections.shuffle(tiles);
        return tiles;
    }

    /**
     * N개 랜덤 선택
     */
    public static List<SingleHexTileType> getRandomTiles(int count) {
        List<SingleHexTileType> shuffled = getShuffledTiles();
        return shuffled.subList(0, Math.min(count, shuffled.size()));
    }

    /**
     * 행성이 있는 타일만 (EMPTY, ASTEROIDS 제외)
     */
    public static List<SingleHexTileType> getPlanetTiles() {
        return List.of(values()).stream()
                .filter(t -> !t.planetType.equals("EMPTY") && !t.planetType.equals("ASTEROIDS"))
                .toList();
    }

    private static final List<String> FORGOTTEN_FLEET_TYPES =
            List.of("TF_MARS", "REBELLION", "ECLIPSE", "TWILIGHT");

    public boolean isForgottenFleet() {
        return FORGOTTEN_FLEET_TYPES.contains(this.planetType);
    }

    /**
     * 잊혀진 함대 우주선만
     */
    public static List<SingleHexTileType> getForgottenFleetTiles() {
        return List.of(values()).stream()
                .filter(t -> FORGOTTEN_FLEET_TYPES.contains(t.planetType))
                .toList();
    }

    /**
     * 잊혀진 함대 우주선 랜덤 N개
     */
    public static List<SingleHexTileType> getRandomForgottenFleet(int count) {
        List<SingleHexTileType> fleets = new ArrayList<>(getForgottenFleetTiles());
        Collections.shuffle(fleets);
        return fleets.subList(0, Math.min(count, fleets.size()));
    }
}
