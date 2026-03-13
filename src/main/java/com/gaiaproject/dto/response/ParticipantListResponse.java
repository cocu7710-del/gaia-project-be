package com.gaiaproject.dto.response;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * 방 참가자 목록 응답 DTO
 */
public record ParticipantListResponse(
        UUID roomId,
        int totalCount,
        List<ParticipantView> participants
) {
    /**
     * 참가자 정보
     */
    public record ParticipantView(
            UUID playerId,
            String nickname,
            Integer claimedSeatNo,  // null이면 좌석 미선택
            String factionName,     // 좌석 선택 시 종족명 (한글)
            LocalDateTime enteredAt
    ) {}
}
