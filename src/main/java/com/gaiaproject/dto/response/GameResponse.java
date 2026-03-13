package com.gaiaproject.dto.response;

import java.util.UUID;

/**
 * 게임 생성/조회 응답 DTO.
 */
public record GameResponse(
        UUID gameId,
        String status
) {}