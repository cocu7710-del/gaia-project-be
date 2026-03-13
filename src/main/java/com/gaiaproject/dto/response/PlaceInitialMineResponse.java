package com.gaiaproject.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "초기 광산 배치 응답")
public record PlaceInitialMineResponse(

        @Schema(description = "게임 ID")
        UUID gameId,

        @Schema(description = "배치된 건물 ID")
        UUID buildingId,

        @Schema(description = "헥스 Q 좌표")
        int hexQ,

        @Schema(description = "헥스 R 좌표")
        int hexR,

        @Schema(description = "배치한 좌석 번호")
        int seatNo,

        @Schema(description = "남은 광산 재고")
        int remainingMines,

        @Schema(description = "초기 배치 완료 여부")
        boolean isSetupComplete,

        @Schema(description = "다음 배치 좌석 번호 (완료 시 null)")
        Integer nextSeatNo,

        @Schema(description = "현재 게임 페이즈")
        String gamePhase,

        @Schema(description = "배치된 건물 타입 (MINE 또는 PLANETARY_INSTITUTE)")
        String buildingType
) {
}
