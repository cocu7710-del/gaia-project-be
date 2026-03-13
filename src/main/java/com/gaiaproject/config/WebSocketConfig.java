package com.gaiaproject.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket STOMP 설정
 * - /ws 엔드포인트로 연결
 * - /topic 으로 메시지 브로드캐스트
 * - /app 으로 메시지 수신
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 클라이언트가 구독할 prefix (서버 -> 클라이언트)
        config.enableSimpleBroker("/topic");
        // 클라이언트가 메시지 보낼 때 prefix (클라이언트 -> 서버)
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // WebSocket 연결 엔드포인트
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")  // CORS 허용
                .withSockJS();                   // SockJS fallback 지원
    }
}
