package com.gaiaproject.domain.entity.map;

import com.gaiaproject.domain.enumtype.map.SectorType;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "game_sector_placement")
@IdClass(GameSectorPlacementId.class)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class GameSectorPlacement {

    @Id
    @Column(name = "game_id")
    private UUID gameId;

    @Id
    @Column(name = "position_no")
    private int positionNo;

    @Column(name = "sector_id", nullable = false)
    private String sectorId;

    @Column(name = "rotation", nullable = false)
    private int rotation;

    public static GameSectorPlacement create(UUID gameId, int positionNo, SectorType sectorType, int rotation) {
        return GameSectorPlacement.builder()
                .gameId(gameId)
                .positionNo(positionNo)
                .sectorId(sectorType.name())
                .rotation(rotation)
                .build();
    }

    /**
     * SectorType enum으로 변환
     */
    public SectorType getSectorType() {
        return SectorType.valueOf(this.sectorId);
    }
}
