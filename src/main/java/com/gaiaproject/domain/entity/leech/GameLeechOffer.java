package com.gaiaproject.domain.entity.leech;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
@Table(name = "game_leech_offer")
public class GameLeechOffer {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "game_id", nullable = false)
    private UUID gameId;

    @Column(name = "batch_key", nullable = false, length = 36)
    private String batchKey;

    @Column(name = "trigger_player_id", nullable = false)
    private UUID triggerPlayerId;

    @Column(name = "receive_player_id", nullable = false)
    private UUID receivePlayerId;

    @Column(name = "receive_seat_no", nullable = false)
    private int receiveSeatNo;

    @Column(name = "power_amount", nullable = false)
    private int powerAmount;

    @Column(name = "vp_cost", nullable = false)
    private int vpCost;

    /** PENDING / ACCEPTED / DECLINED / AUTO_ACCEPTED */
    @Column(name = "status", nullable = false, length = 20)
    private String status;

    /** Taklons-with-PI: gets ordering choice */
    @Column(name = "is_taklons")
    private boolean taklons;

    @Column(name = "sequence_no", nullable = false)
    private int sequenceNo;

    /** null / "TOKEN_FIRST" / "CHARGE_FIRST" — only for Taklons */
    @Column(name = "taklons_choice", length = 20)
    private String taklonsChoice;

    /**
     * Follow-up action after this batch fully resolves.
     * null or "PLACE_MINE_TERRAFORM_2"
     */
    @Column(name = "follow_up_type", length = 50)
    private String followUpType;

    /** JSON data for follow-up action, e.g. {"terraformDiscount":2} */
    @Column(name = "follow_up_data", columnDefinition = "TEXT")
    private String followUpData;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    @Builder
    public GameLeechOffer(UUID gameId, String batchKey, UUID triggerPlayerId,
                          UUID receivePlayerId, int receiveSeatNo,
                          int powerAmount, int vpCost, String status,
                          boolean taklons, int sequenceNo,
                          String followUpType, String followUpData) {
        this.gameId = gameId;
        this.batchKey = batchKey;
        this.triggerPlayerId = triggerPlayerId;
        this.receivePlayerId = receivePlayerId;
        this.receiveSeatNo = receiveSeatNo;
        this.powerAmount = powerAmount;
        this.vpCost = vpCost;
        this.status = status;
        this.taklons = taklons;
        this.sequenceNo = sequenceNo;
        this.followUpType = followUpType;
        this.followUpData = followUpData;
        this.createdAt = LocalDateTime.now();
    }

    public boolean isPending() {
        return "PENDING".equals(status);
    }

    public void accept(String taklonsChoice) {
        this.status = "ACCEPTED";
        this.taklonsChoice = taklonsChoice;
        this.decidedAt = LocalDateTime.now();
    }

    public void decline() {
        this.status = "DECLINED";
        this.decidedAt = LocalDateTime.now();
    }

    public void autoAccept() {
        this.status = "AUTO_ACCEPTED";
        this.decidedAt = LocalDateTime.now();
    }
}
