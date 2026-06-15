package com.gaiaproject.service;

import com.gaiaproject.domain.entity.game.GameVpLog;
import com.gaiaproject.domain.enumtype.action.VpCategory;
import com.gaiaproject.repository.game.GameVpLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class VpLogService {

    private final GameVpLogRepository vpLogRepository;

    /**
     * VP 변동 기록
     */
    public void logVp(UUID gameId, UUID playerId, VpCategory category, int amount, Integer roundNumber, String description) {
        if (amount == 0) return;
        vpLogRepository.save(GameVpLog.builder()
                .gameId(gameId)
                .playerId(playerId)
                .category(category)
                .amount(amount)
                .roundNumber(roundNumber)
                .description(description)
                .build());
        log.debug("[VP_LOG] game={}, player={}, cat={}, amount={}, desc={}", gameId, playerId, category, amount, description);
    }

    /** 세부 VP (라운드별 / 함대별 / 고급타일별 / 최종미션별) */
    public Map<UUID, Map<String, Integer>> getDetailScores(UUID gameId) {
        List<GameVpLog> logs = vpLogRepository.findByGameId(gameId);
        Map<UUID, Map<String, Integer>> result = new LinkedHashMap<>();
        for (GameVpLog log : logs) {
            String key = null;
            if (log.getCategory() == VpCategory.ROUND_SCORING || log.getCategory() == VpCategory.BOOSTER_PASS) {
                key = log.getCategory().name() + "_R" + (log.getRoundNumber() != null ? log.getRoundNumber() : 0);
            } else if (log.getCategory() == VpCategory.FLEET && log.getDescription() != null) {
                String desc = log.getDescription();
                if (desc.contains("ECLIPSE")) key = "FLEET_ECLIPSE";
                else if (desc.contains("TWILIGHT")) key = "FLEET_TWILIGHT";
                else if (desc.contains("TF_MARS")) key = "FLEET_TF_MARS";
                else if (desc.contains("REBELLION")) key = "FLEET_REBELLION";
                else key = "FLEET_OTHER";
            } else if (log.getCategory() == VpCategory.ADV_TECH_TILE && log.getDescription() != null) {
                // description 내 ADV_TILE_NN 패턴 추출
                var m = java.util.regex.Pattern.compile("ADV_TILE_\\d+").matcher(log.getDescription());
                if (m.find()) key = "ADV_TECH_TILE_" + m.group();
                else key = "ADV_TECH_TILE_OTHER";
            } else if (log.getCategory() == VpCategory.FINAL_SCORING && log.getDescription() != null) {
                // description 예시: "최종 미션: FINAL_STRUCTURES (5개)" — FINAL_XXX 추출
                var m = java.util.regex.Pattern.compile("FINAL_[A-Z_]+").matcher(log.getDescription());
                if (m.find()) key = "FINAL_SCORING_" + m.group();
                else key = "FINAL_SCORING_OTHER";
            } else if (log.getCategory() == VpCategory.TECH_TILE && log.getDescription() != null) {
                // 일반 기술타일: BASIC_TILE_N 추출
                var m = java.util.regex.Pattern.compile("BASIC(?:_EXP)?_TILE_\\d+").matcher(log.getDescription());
                if (m.find()) key = "TECH_TILE_" + m.group();
                else if (log.getDescription().contains("가이아 5단계")) key = "TECH_TILE_GAIA_5";
                else key = "TECH_TILE_OTHER";
            } else if (log.getCategory() == VpCategory.FEDERATION_TOKEN && log.getDescription() != null) {
                var m = java.util.regex.Pattern.compile("FED(?:_EXP)?_TILE_\\d+|GLEENS_FEDERATION").matcher(log.getDescription());
                if (m.find()) key = "FEDERATION_TOKEN_" + m.group();
                else key = "FEDERATION_TOKEN_OTHER";
            }
            if (key != null) {
                result.computeIfAbsent(log.getPlayerId(), k -> new LinkedHashMap<>())
                        .merge(key, log.getAmount(), Integer::sum);
            }
        }
        return result;
    }

    /**
     * 게임 결과 조회: 플레이어별 카테고리별 VP 합산
     */
    public Map<UUID, Map<VpCategory, Integer>> getGameResult(UUID gameId) {
        Map<UUID, Map<VpCategory, Integer>> result = new LinkedHashMap<>();
        // 단순 합산 대신 개별 로그에서 FLEET 입장을 FLEET_ENTRY로 재분류
        List<GameVpLog> logs = vpLogRepository.findByGameId(gameId);
        for (GameVpLog log : logs) {
            VpCategory cat = log.getCategory();
            // 기존 FLEET 카테고리 중 입장 비용(마이너스)은 FLEET_ENTRY로 재분류
            if (cat == VpCategory.FLEET && log.getAmount() < 0
                    && log.getDescription() != null && log.getDescription().contains("함대 입장")) {
                cat = VpCategory.FLEET_ENTRY;
            }
            result.computeIfAbsent(log.getPlayerId(), k -> new EnumMap<>(VpCategory.class))
                    .merge(cat, log.getAmount(), Integer::sum);
        }
        return result;
    }
}
