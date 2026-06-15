package com.gaiaproject.config;

import com.gaiaproject.service.GameWebSocketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;
import org.springframework.web.socket.messaging.SessionUnsubscribeEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Slf4j
public class WebSocketEventListener {

    private final GameWebSocketService webSocketService;

    // sessionId → roomId 매핑
    private final Map<String, String> sessionRoomMap = new ConcurrentHashMap<>();
    // roomId → sessionId 집합
    private final Map<String, Set<String>> roomSessionsMap = new ConcurrentHashMap<>();
    // sessionId → playerId (구독 헤더로 받은 값, null 이면 관전자)
    private final Map<String, String> sessionPlayerMap = new ConcurrentHashMap<>();

    @EventListener
    public void handleSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String dest = accessor.getDestination();
        String sessionId = accessor.getSessionId();
        if (dest == null || sessionId == null || !dest.startsWith("/topic/room/")) return;

        String roomId = dest.replace("/topic/room/", "");
        sessionRoomMap.put(sessionId, roomId);
        roomSessionsMap.computeIfAbsent(roomId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);

        // FE 가 subscribe 시 'playerId' 헤더를 보내면 플레이어 세션으로 등록
        // 헤더 없으면 관전자
        List<String> playerIdHeaders = accessor.getNativeHeader("playerId");
        if (playerIdHeaders != null && !playerIdHeaders.isEmpty()) {
            String pid = playerIdHeaders.get(0);
            if (pid != null && !pid.isBlank() && !"null".equals(pid)) {
                sessionPlayerMap.put(sessionId, pid);
            }
        }

        log.info("[WS] Subscribe: session={}, room={}, player={}", sessionId, roomId, sessionPlayerMap.get(sessionId));
        broadcastViewerCount(roomId);
    }

    @EventListener
    public void handleUnsubscribe(SessionUnsubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        removeSession(sessionId);
    }

    @EventListener
    public void handleDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();
        removeSession(sessionId);
    }

    private void removeSession(String sessionId) {
        if (sessionId == null) return;
        String roomId = sessionRoomMap.remove(sessionId);
        sessionPlayerMap.remove(sessionId);
        if (roomId == null) return;
        Set<String> sessions = roomSessionsMap.get(roomId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) roomSessionsMap.remove(roomId);
            log.info("[WS] Disconnect: session={}, room={}", sessionId, roomId);
            broadcastViewerCount(roomId);
        }
    }

    /** 방의 (플레이어 수, 관전자 수) 계산 및 브로드캐스트 */
    private void broadcastViewerCount(String roomId) {
        try {
            UUID gameId = UUID.fromString(roomId);
            Set<String> sessions = roomSessionsMap.getOrDefault(roomId, Set.of());
            // 플레이어 세션: playerId 헤더가 있는 것 (고유 playerId 기준 중복 제거)
            Set<String> distinctPlayers = sessions.stream()
                    .map(sessionPlayerMap::get)
                    .filter(p -> p != null && !p.isBlank())
                    .collect(java.util.stream.Collectors.toSet());
            int playerCount = distinctPlayers.size();
            int totalSessions = sessions.size();
            // 관전자 세션 = 전체 세션 - 플레이어 세션 (playerId 헤더 가진 세션 수, 중복 미제거)
            long playerSessionCount = sessions.stream()
                    .map(sessionPlayerMap::get)
                    .filter(p -> p != null && !p.isBlank())
                    .count();
            int viewerCount = Math.max(0, totalSessions - (int) playerSessionCount);

            Map<String, Object> payload = new HashMap<>();
            payload.put("count", totalSessions);       // 하위호환: 전체 세션 수
            payload.put("players", playerCount);        // 고유 플레이어 수
            payload.put("viewers", viewerCount);        // 관전자 수
            webSocketService.broadcast(
                com.gaiaproject.dto.websocket.GameEvent.of(gameId, "VIEWER_COUNT", payload)
            );
        } catch (Exception ignored) {}
    }

    /** 특정 방의 전체 세션 수 조회 */
    public int getViewerCount(String roomId) {
        Set<String> sessions = roomSessionsMap.get(roomId);
        return sessions != null ? sessions.size() : 0;
    }
}
