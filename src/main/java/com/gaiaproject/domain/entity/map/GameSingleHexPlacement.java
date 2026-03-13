package com.gaiaproject.domain.entity.map;

import com.gaiaproject.domain.enumtype.map.SingleHexTileType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "game_single_hex_placement")
@IdClass(GameSingleHexPlacementId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GameSingleHexPlacement {

    @Id
    @Column(name = "game_id")
    private UUID gameId;

    @Id
    @Column(name = "position_no")
    private int positionNo;

    @Column(name = "tile_type", nullable = false)
    private String tileType;

    public static GameSingleHexPlacement create(UUID gameId, int positionNo, SingleHexTileType tileType) {
        return GameSingleHexPlacement.builder()
                .gameId(gameId)
                .positionNo(positionNo)
                .tileType(tileType.name())
                .build();
    }

    /**
     * SingleHexTileType enum으로 변환
     */
    public SingleHexTileType getTileTypeEnum() {
        return SingleHexTileType.valueOf(this.tileType);
    }
}
