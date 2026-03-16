package com.gaiaproject.service;

import com.gaiaproject.dto.websocket.GameEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.UUID;

/**
 * WebSocket 브로드캐스트 서비스
 * - 게임 이벤트를 해당 방의 모든 클라이언트에게 전송
 * - 트랜잭션 내에서 호출 시 커밋 후 전송 (클라이언트가 최신 데이터를 조회할 수 있도록)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GameWebSocketService {

    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 특정 방의 모든 클라이언트에게 이벤트 전송
     * - 트랜잭션 활성 시: 커밋 후 전송
     * - 트랜잭션 비활성 시: 즉시 전송
     */
    public void broadcast(GameEvent event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendEvent(event);
                }
            });
        } else {
            sendEvent(event);
        }
    }

    private void sendEvent(GameEvent event) {
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
    public void broadcastPlayerPassed(UUID roomId, UUID playerId, int seatNo, boolean allPassed) {
        broadcast(GameEvent.of(roomId, "PLAYER_PASSED",
                java.util.Map.of("playerId", playerId.toString(), "seatNo", seatNo, "allPassed", allPassed)));
    }

    /**
     * 라운드 시작 이벤트
     */
    public void broadcastRoundStarted(UUID roomId, int roundNumber) {
        broadcast(GameEvent.of(roomId, "ROUND_STARTED",
                java.util.Map.of("roundNumber", roundNumber)));
    }

    /**
     * 파워 리치 결정 요청 이벤트 (동시 결정 - 모든 결정자에게 동시 전달)
     */
    public void broadcastLeechOfferedAll(UUID roomId, String batchKey,
                                          java.util.List<java.util.Map<String, Object>> offers,
                                          java.util.List<String> deciderIds) {
        broadcast(GameEvent.of(roomId, "LEECH_OFFERED", java.util.Map.of(
                "batchKey", batchKey,
                "offers", offers,
                "deciderIds", deciderIds
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
     * 팅커로이드 PI: 라운드 시작 시 액션 타일 선택 요청
     */
    public void broadcastTinkeroidsActionChoice(UUID roomId, UUID tinkeroidsPlayerId, java.util.List<String> availableActions, int currentRound) {
        broadcast(GameEvent.of(roomId, "TINKEROIDS_ACTION_CHOICE", java.util.Map.of(
                "tinkeroidsPlayerId", tinkeroidsPlayerId.toString(),
                "availableActions", availableActions,
                "currentRound", currentRound
        )));
    }

    /**
     * 아이타 PI: 라운드 종료 시 가이아→기술타일 선택 요청
     */
    public void broadcastItarsGaiaChoice(UUID roomId, UUID itarsPlayerId, int availableChoices) {
        broadcast(GameEvent.of(roomId, "ITARS_GAIA_CHOICE", java.util.Map.of(
                "itarsPlayerId", itarsPlayerId.toString(),
                "availableChoices", availableChoices
        )));
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
