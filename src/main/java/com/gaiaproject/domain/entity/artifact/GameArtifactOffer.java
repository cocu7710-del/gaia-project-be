package com.gaiaproject.domain.entity.artifact;

import com.gaiaproject.domain.enumtype.artifact.ArtifactType;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "game_artifact_offer")
public class GameArtifactOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Enumerated(EnumType.STRING)
    @Column(name = "artifact_type", nullable = false, length = 50)
    private ArtifactType artifactType;

    @Column(name = "position", nullable = false)
    private Integer position;

    @Column(name = "is_acquired", nullable = false)
    private Boolean isAcquired = false;

    @Column(name = "acquired_by")
    private UUID acquiredBy;

    @Column(name = "acquired_at")
    private LocalDateTime acquiredAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Builder
    public GameArtifactOffer(UUID gameId, ArtifactType artifactType, Integer position) {
        this.gameId = gameId;
        this.artifactType = artifactType;
        this.position = position;
        this.isAcquired = false;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 인공물 획득 처리
     */
    public void acquire(UUID playerId) {
        if (this.isAcquired) {
            throw new IllegalStateException("이미 획득된 인공물입니다");
        }
        this.isAcquired = true;
        this.acquiredBy = playerId;
        this.acquiredAt = LocalDateTime.now();
    }
}