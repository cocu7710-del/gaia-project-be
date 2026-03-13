package com.gaiaproject.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

@Schema(description = "초기 광산 배치 요청")
public record PlaceInitialMineRequest(

        @NotNull
        @Schema(description = "플레이어 ID", example = "550e8400-e29b-41d4-a716-446655440000")
        UUID playerId,

        @NotNull
        @Schema(description = "헥스 Q 좌표", example = "3")
        Integer hexQ,

        @NotNull
        @Schema(description = "헥스 R 좌표", example = "-2")
        Integer hexR
) {
}
