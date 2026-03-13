package com.gaiaproject.domain.entity.player;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 사용자(플레이어) 엔티티.
 * - 지금은 닉네임만, 나중에 인증/JWT 붙으면 계정과 분리 가능.
 */
@Getter
@NoArgsConstructor(access = lombok.AccessLevel.PROTECTED)
@Entity
@Table(name = "player")
public class Player {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String nickname;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static Player create(String nickname) {
        Player p = new Player();
        p.nickname = nickname;
        p.createdAt = LocalDateTime.now();
        return p;
    }
}
