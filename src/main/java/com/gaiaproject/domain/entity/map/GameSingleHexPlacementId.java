package com.gaiaproject.domain.entity.map;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class GameSingleHexPlacementId implements Serializable {
    private UUID gameId;
    private int positionNo;
}
