package com.gaiaproject.service;

import com.gaiaproject.domain.entity.player.GamePlayerState;
import com.gaiaproject.repository.player.GamePlayerStateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * 프리 액션: 자원 변환 (턴 소모 없음)
 * - ORE_TO_CREDIT     : 광석 1 → 크레딧 1
 * - ORE_TO_TOKEN      : 광석 1 → 파워토큰 1 (bowl1)
 * - POWER_TO_CREDIT   : 파워 1 → 크레딧 1
 * - POWER_TO_ORE      : 파워 3 → 광석 1
 * - POWER_TO_KNOWLEDGE: 파워 4 → 지식 1
 * - POWER_TO_QIC      : 파워 4 → QIC 1
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class FreeConvertService {

    private final GamePlayerStateRepository playerStateRepository;

    public record FreeConvertResponse(boolean success, String message) {
        static FreeConvertResponse ok() { return new FreeConvertResponse(true, null); }
        static FreeConvertResponse fail(String msg) { return new FreeConvertResponse(false, msg); }
    }

    public FreeConvertResponse convert(UUID gameId, UUID playerId, String convertCode) {
        GamePlayerState ps = playerStateRepository.findByGameIdAndPlayerId(gameId, playerId)
                .orElseThrow(() -> new IllegalArgumentException("플레이어 상태 없음: " + playerId));

        try {
            switch (convertCode) {
                case "ORE_TO_CREDIT" -> {
                    ps.spendOre(1);
                    ps.addCredit(1);
                    log.info("[FREE] 광석→크레딧: player={}", playerId);
                }
                case "ORE_TO_TOKEN" -> {
                    ps.spendOre(1);
                    ps.addPowerToken(1);
                    log.info("[FREE] 광석→파워토큰: player={}", playerId);
                }
                case "ORE_TO_POWER3" -> {
                    ps.spendOre(1);
                    ps.gainPower(1);  // bowl3에 직접 추가
                    log.info("[FREE] 광석→3구역파워(XENOS): player={}", playerId);
                }
                case "POWER_TO_CREDIT" -> {
                    ps.spendPower(1);
                    ps.addCredit(1);
                    log.info("[FREE] 파워1→크레딧: player={}", playerId);
                }
                case "POWER_TO_ORE" -> {
                    ps.spendPower(3);
                    ps.addOre(1);
                    log.info("[FREE] 파워3→광석: player={}", playerId);
                }
                case "POWER_TO_KNOWLEDGE" -> {
                    ps.spendPower(4);
                    ps.addKnowledge(1);
                    log.info("[FREE] 파워4→지식: player={}", playerId);
                }
                case "POWER_TO_QIC" -> {
                    ps.spendPower(4);
                    ps.addQic(1);
                    log.info("[FREE] 파워4→QIC: player={}", playerId);
                }
                default -> { return FreeConvertResponse.fail("알 수 없는 변환 코드: " + convertCode); }
            }
        } catch (IllegalStateException e) {
            return FreeConvertResponse.fail(e.getMessage());
        }

        playerStateRepository.save(ps);
        return FreeConvertResponse.ok();
    }
}
