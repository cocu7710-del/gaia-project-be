package com.gaiaproject.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

/** 방 생성 요청 DTO */
public record CreateRoomRequest(

        @Schema(description = "방 제목", example = "TestGameRoom1")
        @NotBlank String title
) {}
