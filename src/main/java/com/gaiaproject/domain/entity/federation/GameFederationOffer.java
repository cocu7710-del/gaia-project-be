package com.gaiaproject.domain.entity.federation;

import com.gaiaproject.domain.enumtype.federation.FederationTileType;
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
@Table(name = "game_federation_offer")
public class GameFederationOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Enumerated(EnumType.STRING)
    @Column(name = "federation_tile_type", nullable = false, length = 50)
    private FederationTileType federationTileType;

    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    @Column(name = "position")
    private Integer position;  // null: 일반 공급처, 1~4: 잊힌 함대, 0: 테라포밍 트랙

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Builder
    public GameFederationOffer(UUID gameId, FederationTileType federationTileType, Integer quantity, Integer position) {
        this.gameId = gameId;
        this.federationTileType = federationTileType;
        this.quantity = quantity;
        this.position = position;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public void decreaseQuantity() {
        if (this.quantity <= 0) {
            throw new IllegalStateException("연방 타일이 모두 소진되었습니다");
        }
        this.quantity--;
    }
}