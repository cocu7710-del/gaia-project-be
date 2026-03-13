package com.gaiaproject.dto.response;

import com.gaiaproject.domain.enumtype.player.FactionType;

import java.util.List;
import java.util.UUID;

/** 방 생성 응답 DTO */
public record CreateRoomResponse(
        UUID roomId,
        String title,
        String roomCode,
        String status,
        List<FactionType> raceList
) {}
