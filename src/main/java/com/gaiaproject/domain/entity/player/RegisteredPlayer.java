package com.gaiaproject.domain.entity.player;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 등록된 플레이어 엔티티 (닉네임 화이트리스트).
 * - 직접 DB INSERT로만 등록 가능.
 * - 방 생성/입장 시 닉네임이 이 테이블에 있어야 플레이 가능.
 */
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Entity
@Table(name = "registered_player")
public class RegisteredPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100, unique = true)
    private String nickname;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** 실제 플레이어 여부. false면 테스트 계정으로 전적 집계에서 제외됨 */
    @Column(name = "is_real_player", nullable = false)
    private boolean isRealPlayer = true;
}
