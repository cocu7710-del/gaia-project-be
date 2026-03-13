package com.gaiaproject.service;

import com.gaiaproject.domain.entity.game.Game;
import com.gaiaproject.domain.entity.game.GameSeat;
import com.gaiaproject.domain.entity.building.GameBuilding;
import com.gaiaproject.domain.entity.leech.GameLeechOffer;
import com.gaiaproject.domain.entity.player.GamePlayerState;
import com.gaiaproject.domain.enumtype.building.BuildingType;
import com.gaiaproject.domain.enumtype.player.FactionType;
import com.gaiaproject.repository.game.GameRepository;
import com.gaiaproject.repository.game.GameSeatRepository;
import com.gaiaproject.repository.leech.GameLeechOfferRepository;
import com.gaiaproject.repository.player.GamePlayerStateRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PowerLeechService {

    private final GameLeechOfferRepository leechOfferRepository;
    private final GamePlayerStateRepository playerStateRepository;
    private final GameSeatRepository seatRepository;
    private final GameRepository gameRepository;
    private final GameWebSocketService webSocketService;
    private final ActionService actionService;

    /**
     * 건물 배치/업그레이드 후 파워 리치 배치 처리.
     * - 자동 결정 케이스는 즉시 처리
     * - 수동 결정 필요 케이스는 PENDING offer 생성
     * - PENDING이 있으면 LEECH_OFFERED 브로드캐스트, 없으면 턴 진행
     *
     * @param followUpType  이 배치 리치 해소 후 발생할 후속 액션 타입 (null 가능)
     * @param followUpData  후속 액션 JSON 데이터 (null 가능)
     */
    public void createBatchAndProcess(Game game, UUID triggerPlayerId,
                                       int hexQ, int hexR, BuildingType newType,
                                       List<GameBuilding> allBuildings,
                                       String followUpType, String followUpData) {
        if (buildingPowerValue(newType) == 0) {
            // 파워 값 0인 건물 (가이아포머 등) → 리치 없음 → 바로 턴 진행
            if (followUpType != null) {
                broadcastFollowUp(game.getId(), triggerPlayerId, followUpType, followUpData);
            } else {
                actionService.advanceTurnAndBroadcast(game);
            }
            return;
        }

        UUID gameId = game.getId();
        String batchKey = UUID.randomUUID().toString();

        // 트리거 플레이어의 좌석 번호 조회
        int triggerSeatNo = seatRepository.findByGameIdAndPlayerId(gameId, triggerPlayerId)
                .map(GameSeat::getSeatNo)
                .orElse(1);

        // 2거리 이내 상대 플레이어별 최대 파워 값 계산
        Map<UUID, Integer> maxPowerByPlayer = new HashMap<>();
        for (GameBuilding b : allBuildings) {
            if (b.getPlayerId().equals(triggerPlayerId)) continue;
            int dist = hexDistance(hexQ, hexR, b.getHexQ(), b.getHexR());
            if (dist > 2) continue;
            int pv = buildingPowerValue(b.getBuildingType());
            maxPowerByPlayer.merge(b.getPlayerId(), pv, Math::max);
        }

        if (maxPowerByPlayer.isEmpty()) {
            if (followUpType != null) {
                broadcastFollowUp(gameId, triggerPlayerId, followUpType, followUpData);
            } else {
                actionService.advanceTurnAndBroadcast(game);
            }
            return;
        }

        List<GameSeat> seats = seatRepository.findByGameIdOrderBySeatNo(gameId);
        int maxSeatNo = seats.size();
        List<GamePlayerState> allStates = playerStateRepository.findByGameId(gameId);

        List<GameLeechOffer> pendingOffers = new ArrayList<>();
        int seqNo = 0;
        boolean isFirstOffer = true;

        // 트리거 플레이어 다음 좌석부터 순서대로 처리
        for (int i = 1; i <= maxSeatNo; i++) {
            int checkSeatNo = ((triggerSeatNo - 1 + i) % maxSeatNo) + 1;

            GameSeat seat = seats.stream()
                    .filter(s -> s.getSeatNo() == checkSeatNo)
                    .findFirst().orElse(null);
            if (seat == null || seat.getPlayerId() == null) continue;

            Integer power = maxPowerByPlayer.get(seat.getPlayerId());
            if (power == null || power == 0) continue;

            GamePlayerState ps = allStates.stream()
                    .filter(s -> s.getPlayerId().equals(seat.getPlayerId()))
                    .findFirst().orElse(null);
            if (ps == null) continue;

            FactionType faction = resolveFaction(gameId, ps);
            boolean isTaklonsPI = faction == FactionType.TAKLONS && ps.getStockPlanetaryInstitute() == 0;
            boolean canDeclineOne = faction == FactionType.ITARS || isTaklonsPI;
            int vpCost = Math.max(0, power - 1);

            // 규칙 2-6: VP 부족 시 1파워 무료 자동 수령
            if (vpCost > 0 && ps.getVictoryPoints() < vpCost) {
                applyPowerToPlayer(ps, 1, 0, isTaklonsPI, null);
                playerStateRepository.save(ps);
                saveOffer(gameId, batchKey, triggerPlayerId, seat.getPlayerId(), checkSeatNo,
                        1, 0, "AUTO_ACCEPTED", seqNo++, false,
                        isFirstOffer ? followUpType : null,
                        isFirstOffer ? followUpData : null);
                isFirstOffer = false;
                log.info("[LEECH] VP 부족 자동 1파워: player={}, vp={}, needed={}", seat.getPlayerId(), ps.getVictoryPoints(), vpCost);
                continue;
            }

            // 규칙 2-2: 1파워는 특수 종족 제외 자동 수령
            if (power == 1 && !canDeclineOne) {
                applyPowerToPlayer(ps, 1, 0, isTaklonsPI, null);
                playerStateRepository.save(ps);
                saveOffer(gameId, batchKey, triggerPlayerId, seat.getPlayerId(), checkSeatNo,
                        1, 0, "AUTO_ACCEPTED", seqNo++, false,
                        isFirstOffer ? followUpType : null,
                        isFirstOffer ? followUpData : null);
                isFirstOffer = false;
                log.info("[LEECH] 1파워 자동 수령: player={}", seat.getPlayerId());
                continue;
            }

            // 수동 결정 필요
            GameLeechOffer offer = saveOffer(gameId, batchKey, triggerPlayerId, seat.getPlayerId(), checkSeatNo,
                    power, vpCost, "PENDING", seqNo++, isTaklonsPI,
                    isFirstOffer ? followUpType : null,
                    isFirstOffer ? followUpData : null);
            isFirstOffer = false;
            pendingOffers.add(offer);
            log.info("[LEECH] 수동 결정 대기: player={}, power={}, vpCost={}", seat.getPlayerId(), power, vpCost);
        }

        if (pendingOffers.isEmpty()) {
            // 모든 자동 처리 완료 → 턴 또는 후속 액션 진행
            String resolvedFollowUpType = leechOfferRepository
                    .findByGameIdAndBatchKeyOrderBySequenceNo(gameId, batchKey)
                    .stream().findFirst()
                    .map(GameLeechOffer::getFollowUpType).orElse(null);

            if (resolvedFollowUpType != null) {
                String resolvedFollowUpData = leechOfferRepository
                        .findByGameIdAndBatchKeyOrderBySequenceNo(gameId, batchKey)
                        .stream().findFirst()
                        .map(GameLeechOffer::getFollowUpData).orElse(null);
                broadcastFollowUp(gameId, triggerPlayerId, resolvedFollowUpType, resolvedFollowUpData);
            } else {
                actionService.advanceTurnAndBroadcast(game);
            }
        } else {
            // 첫 번째 PENDING offer에게 LEECH_OFFERED 브로드캐스트
            broadcastLeechOffered(gameId, batchKey, pendingOffers, allStates);
        }
    }

    /**
     * 플레이어의 파워 리치 결정 처리
     */
    public void decidePowerLeech(UUID gameId, UUID leechId, UUID decidingPlayerId,
                                  boolean accept, String taklonsChoice) {
        GameLeechOffer offer = leechOfferRepository.findById(leechId)
                .orElseThrow(() -> new IllegalArgumentException("리치 오퍼를 찾을 수 없습니다"));

        if (!offer.getReceivePlayerId().equals(decidingPlayerId)) {
            throw new IllegalStateException("이 리치를 결정할 권한이 없습니다");
        }
        if (!offer.isPending()) {
            throw new IllegalStateException("이미 결정된 리치입니다");
        }

        if (accept) {
            GamePlayerState ps = playerStateRepository
                    .findByGameIdAndPlayerId(gameId, offer.getReceivePlayerId())
                    .orElseThrow(() -> new IllegalStateException("플레이어 상태를 찾을 수 없습니다"));

            applyPowerToPlayer(ps, offer.getPowerAmount(), offer.getVpCost(), offer.isTaklons(), taklonsChoice);
            playerStateRepository.save(ps);
            offer.accept(taklonsChoice);
            log.info("[LEECH] 수락: leechId={}, player={}, power={}, vpCost={}, taklonsChoice={}",
                    leechId, decidingPlayerId, offer.getPowerAmount(), offer.getVpCost(), taklonsChoice);
        } else {
            offer.decline();
            log.info("[LEECH] 거절: leechId={}, player={}", leechId, decidingPlayerId);
        }
        leechOfferRepository.save(offer);

        // 다음 PENDING offer 탐색
        GameLeechOffer nextPending = leechOfferRepository
                .findFirstByGameIdAndBatchKeyAndStatusOrderBySequenceNo(gameId, offer.getBatchKey(), "PENDING")
                .orElse(null);

        if (nextPending != null) {
            // 다음 결정자에게 이벤트 전달
            webSocketService.broadcastLeechDecided(gameId, offer, nextPending);
        } else {
            // 배치 완료 → 후속 액션 또는 턴 진행
            String followUpType = leechOfferRepository
                    .findByGameIdAndBatchKeyOrderBySequenceNo(gameId, offer.getBatchKey())
                    .stream().findFirst()
                    .map(GameLeechOffer::getFollowUpType).orElse(null);

            if (followUpType != null) {
                String followUpData = leechOfferRepository
                        .findByGameIdAndBatchKeyOrderBySequenceNo(gameId, offer.getBatchKey())
                        .stream().findFirst()
                        .map(GameLeechOffer::getFollowUpData).orElse(null);
                webSocketService.broadcastLeechDecided(gameId, offer, null);
                Game game = gameRepository.findById(gameId)
                        .orElseThrow(() -> new IllegalStateException("게임을 찾을 수 없습니다"));
                UUID triggerPlayerId = resolveTriggerPlayerId(gameId,
                        game.getCurrentTurnSeatNo() != null ? game.getCurrentTurnSeatNo() : 1);
                if (triggerPlayerId == null) triggerPlayerId = offer.getTriggerPlayerId();
                broadcastFollowUp(gameId, triggerPlayerId, followUpType, followUpData);
            } else {
                Game game = gameRepository.findById(gameId)
                        .orElseThrow(() -> new IllegalStateException("게임을 찾을 수 없습니다"));
                webSocketService.broadcastLeechDecided(gameId, offer, null);
                actionService.advanceTurnAndBroadcast(game);
            }
        }
    }

    /**
     * 현재 게임의 대기 중인 리치 오퍼 조회 (페이지 리프레시 복구용)
     */
    public List<GameLeechOffer> getPendingOffers(UUID gameId) {
        return leechOfferRepository.findByGameIdAndStatus(gameId, "PENDING");
    }

    // ========== private helpers ==========

    private void applyPowerToPlayer(GamePlayerState ps, int power, int vpCost,
                                     boolean isTaklons, String taklonsChoice) {
        if (isTaklons && "TOKEN_FIRST".equals(taklonsChoice)) {
            ps.addPowerToken(1);
            ps.chargePowerWithFactionRules(power);
        } else {
            ps.chargePowerWithFactionRules(power);
            if (isTaklons) ps.addPowerToken(1);
        }
        if (vpCost > 0) ps.addVP(-vpCost);
    }

    private GameLeechOffer saveOffer(UUID gameId, String batchKey, UUID triggerPlayerId,
                                      UUID receivePlayerId, int receiveSeatNo,
                                      int powerAmount, int vpCost, String status,
                                      int seqNo, boolean isTaklons,
                                      String followUpType, String followUpData) {
        GameLeechOffer offer = GameLeechOffer.builder()
                .gameId(gameId).batchKey(batchKey)
                .triggerPlayerId(triggerPlayerId).receivePlayerId(receivePlayerId)
                .receiveSeatNo(receiveSeatNo).powerAmount(powerAmount).vpCost(vpCost)
                .status(status).taklons(isTaklons).sequenceNo(seqNo)
                .followUpType(followUpType).followUpData(followUpData)
                .build();
        return leechOfferRepository.save(offer);
    }

    private void broadcastLeechOffered(UUID gameId, String batchKey,
                                        List<GameLeechOffer> pendingOffers,
                                        List<GamePlayerState> allStates) {
        GameLeechOffer firstPending = pendingOffers.get(0);
        List<Map<String, Object>> offerList = pendingOffers.stream()
                .map(o -> {
                    Map<String, Object> m = new java.util.LinkedHashMap<>();
                    m.put("id", o.getId().toString());
                    m.put("receivePlayerId", o.getReceivePlayerId().toString());
                    m.put("receiveSeatNo", o.getReceiveSeatNo());
                    m.put("powerAmount", o.getPowerAmount());
                    m.put("vpCost", o.getVpCost());
                    m.put("isTaklons", o.isTaklons());
                    return m;
                }).toList();

        webSocketService.broadcastLeechOffered(gameId, batchKey,
                firstPending.getId().toString(),
                firstPending.getReceivePlayerId().toString(),
                offerList);
    }

    private void broadcastFollowUp(UUID gameId, UUID triggerPlayerId, String followUpType, String followUpData) {
        webSocketService.broadcastDeferredActionRequired(gameId, triggerPlayerId, followUpType, followUpData);
    }

    private FactionType resolveFaction(UUID gameId, GamePlayerState ps) {
        if (ps.getFactionType() != null) return ps.getFactionType();
        return seatRepository.findByGameIdAndSeatNo(gameId, ps.getSeatNo())
                .map(GameSeat::getFactionType).orElse(null);
    }

    private UUID resolveTriggerPlayerId(UUID gameId, int seatNo) {
        return seatRepository.findByGameIdAndSeatNo(gameId, seatNo)
                .map(GameSeat::getPlayerId).orElse(null);
    }

    private int buildingPowerValue(BuildingType type) {
        return switch (type) {
            case MINE -> 1;
            case TRADING_STATION, RESEARCH_LAB -> 2;
            case PLANETARY_INSTITUTE, ACADEMY -> 3;
            default -> 0;
        };
    }

    private int hexDistance(int q1, int r1, int q2, int r2) {
        int dq = q2 - q1, dr = r2 - r1;
        return (Math.abs(dq) + Math.abs(dr) + Math.abs(dq + dr)) / 2;
    }
}
