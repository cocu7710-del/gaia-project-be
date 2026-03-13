package com.gaiaproject.dto.request;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/**
 * 게임 입장 요청 DTO.
 * - 닉네임만 받고, 좌석 선택은 별도 API에서 처리
 */
public record EnterGameRequest(

        @Schema(description = "닉네임", example = "testUser01")
        String nickname,

        @Schema(description = "재입장 토큰 (재입장 시 필요)", example = "A1B2C3D4")
        String rejoinToken
) {}
