package com.gaiaproject.dto.response;

import java.util.UUID;

/**
 * 참가자 검증 응답 DTO.
 * - 재입장 시 playerId가 유효한지 확인하고, 기존 상태를 반환한다.
 */
public record VerifyParticipantResponse(
        UUID roomId,
        UUID playerId,
        boolean valid,
        String nickname,
        Integer seatNo,
        String factionName,
        String message
) {
    public static VerifyParticipantResponse valid(
            UUID roomId,
            UUID playerId,
            String nickname,
            Integer seatNo,
            String factionName
    ) {
        return new VerifyParticipantResponse(roomId, playerId, true, nickname, seatNo, factionName, null);
    }

    public static VerifyParticipantResponse invalid(UUID roomId, UUID playerId, String message) {
        return new VerifyParticipantResponse(roomId, playerId, false, null, null, null, message);
    }
}
