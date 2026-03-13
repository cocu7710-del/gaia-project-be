package com.gaiaproject.service;

import com.gaiaproject.dto.websocket.GameEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * WebSocket 브로드캐스트 서비스
 * - 게임 이벤트를 해당 방의 모든 클라이언트에게 전송
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 특정 방의 모든 클라이언트에게 이벤트 전송
     * - 클라이언트는 /topic/room/{roomId} 를 구독해야 함
     */
    public void broadcast(GameEvent event) {
        String destination = "/topic/room/" + event.roomId();
        log.info("Broadcasting event: {} to {}", event.eventType(), destination);
        messagingTemplate.convertAndSend(destination, event);
    }

    /**
     * 플레이어 입장 이벤트
     */
    public void broadcastPlayerJoined(UUID roomId, UUID playerId, String nickname) {
        broadcast(GameEvent.playerJoined(roomId, playerId, nickname));
    }

    /**
     * 좌석 선택 이벤트
     */
    public void broadcastSeatClaimed(UUID roomId, UUID playerId, int seatNo, String factionName) {
        broadcast(GameEvent.seatClaimed(roomId, playerId, seatNo, factionName));
    }

    /**
     * 부스터 선택 이벤트
     */
    public void broadcastBoosterSelected(UUID roomId, UUID playerId, int seatNo, String boosterCode, int nextPickSeatNo) {
        broadcast(GameEvent.boosterSelected(roomId, playerId, seatNo, boosterCode, nextPickSeatNo));
    }

    /**
     * 게임 시작 이벤트
     */
    public void broadcastGameStarted(UUID roomId, String gamePhase, Integer nextSetupSeatNo) {
        broadcast(GameEvent.gameStarted(roomId, gamePhase, nextSetupSeatNo));
    }

    /**
     * 광산 배치 이벤트
     */
    public void broadcastMinePlaced(UUID roomId, UUID playerId, int seatNo, int hexQ, int hexR, Integer nextSeatNo, String gamePhase) {
        broadcast(GameEvent.minePlaced(roomId, playerId, seatNo, hexQ, hexR, nextSeatNo, gamePhase));
    }

    /**
     * 상태 갱신 이벤트 (범용)
     */
    public void broadcastStateUpdated(UUID roomId) {
        broadcast(GameEvent.stateUpdated(roomId));
    }

    /**
     * 턴 변경 이벤트
     */
    public void broadcastTurnChanged(UUID roomId, int newTurnSeatNo) {
        broadcast(GameEvent.of(roomId, "TURN_CHANGED",
                java.util.Map.of("newTurnSeatNo", newTurnSeatNo)));
    }

    /**
     * 플레이어 패스 이벤트
     */
    public void broadcastPlayerPassed(UUID roomId, UUID playerId, boolean allPassed) {
        broadcast(GameEvent.of(roomId, "PLAYER_PASSED",
                java.util.Map.of("playerId", playerId.toString(), "allPassed", allPassed)));
    }

    /**
     * 라운드 시작 이벤트
     */
    public void broadcastRoundStarted(UUID roomId, int roundNumber) {
        broadcast(GameEvent.of(roomId, "ROUND_STARTED",
                java.util.Map.of("roundNumber", roundNumber)));
    }

    /**
     * 파워 리치 결정 요청 이벤트
     */
    public void broadcastLeechOffered(UUID roomId, String batchKey, String currentLeechId,
                                       String currentDeciderId, java.util.List<java.util.Map<String, Object>> offers) {
        broadcast(GameEvent.of(roomId, "LEECH_OFFERED", java.util.Map.of(
                "batchKey", batchKey,
                "currentLeechId", currentLeechId,
                "currentDeciderId", currentDeciderId,
                "offers", offers
        )));
    }

    /**
     * 파워 리치 결정 완료 이벤트
     */
    public void broadcastLeechDecided(UUID roomId, com.gaiaproject.domain.entity.leech.GameLeechOffer decided,
                                       com.gaiaproject.domain.entity.leech.GameLeechOffer nextOffer) {
        java.util.Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("decidedLeechId", decided.getId().toString());
        payload.put("accepted", "ACCEPTED".equals(decided.getStatus()));
        payload.put("allResolved", nextOffer == null);
        if (nextOffer != null) {
            payload.put("nextLeechId", nextOffer.getId().toString());
            payload.put("nextDeciderId", nextOffer.getReceivePlayerId().toString());
        }
        broadcast(GameEvent.of(roomId, "LEECH_DECIDED", payload));
    }

    /**
     * 후속 액션 필요 이벤트 (예: 2삽 광산)
     */
    public void broadcastDeferredActionRequired(UUID roomId, UUID triggerPlayerId,
                                                 String actionType, String actionData) {
        broadcast(GameEvent.of(roomId, "DEFERRED_ACTION_REQUIRED", java.util.Map.of(
                "triggerPlayerId", triggerPlayerId != null ? triggerPlayerId.toString() : "",
                "actionType", actionType,
                "actionData", actionData != null ? actionData : "{}"
        )));
    }

}
