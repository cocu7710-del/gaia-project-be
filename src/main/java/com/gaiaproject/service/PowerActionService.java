package com.gaiaproject.service;

import com.gaiaproject.domain.entity.game.Game;
import com.gaiaproject.domain.entity.game.GamePowerActionUsage;
import com.gaiaproject.domain.entity.player.GamePlayerState;
import com.gaiaproject.domain.enumtype.action.ActionType;
import com.gaiaproject.domain.enumtype.action.PowerActionType;
import com.gaiaproject.dto.request.UsePowerActionRequest;
import com.gaiaproject.dto.response.ConfirmActionResponse;
import com.gaiaproject.dto.response.UsePowerActionResponse;
import com.gaiaproject.domain.entity.game.GameAction;
import com.gaiaproject.repository.game.GameActionRepository;
import com.gaiaproject.repository.game.GamePowerActionUsageRepository;
import com.gaiaproject.repository.game.GameRepository;
import com.gaiaproject.repository.player.GamePlayerStateRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class PowerActionService {

    private final GameRepository gameRepository;
    private final GamePlayerStateRepository playerStateRepository;
    private final GamePowerActionUsageRepository powerActionUsageRepository;
    private final GameActionRepository gameActionRepository;
    private final ActionService actionService;

    private record PowerActionEffect(int powerCost, int creditsGain, int oreGain, int knowledgeGain, int qicGain, int powerTokenGain) {
        PowerActionEffect(int powerCost, int creditsGain, int oreGain, int knowledgeGain, int qicGain) {
            this(powerCost, creditsGain, oreGain, knowledgeGain, qicGain, 0);
        }
    }

    /** FE 코드 → PowerActionType (enum key = FE 코드이므로 valueOf 사용) */
    private PowerActionType toUsageType(String code) {
        try {
            return PowerActionType.valueOf(code);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    /** 기본 파워 액션 효과 정의 (FE 코드 기준, 잘못된 gain 모두 수정) */
    private PowerActionEffect getEffect(String code) {
        return switch (code) {
            case "PWR_KNOWLEDGE" -> new PowerActionEffect(7, 0, 0, 3, 0);
            case "PWR_TERRAFORM_2" -> new PowerActionEffect(5, 0, 0, 0, 0);
            case "PWR_ORE"         -> new PowerActionEffect(4, 0, 2, 0, 0);
            case "PWR_CREDIT"    -> new PowerActionEffect(4, 7, 0, 0, 0);
            case "PWR_KNOWLEDGE_2" -> new PowerActionEffect(4, 0, 0, 2, 0);
            case "PWR_TERRAFORM" -> new PowerActionEffect(3, 0, 0, 0, 0);
            case "PWR_TOKEN"     -> new PowerActionEffect(3, 0, 0, 0, 0, 2);
            default -> null; // 함대 액션은 FleetService에서 처리
        };
    }

    /** 파워 소각: bowl2에서 2개 제거, bowl3에 1개 추가 (자유 행동 - 턴 종료 없음) */
    public void burnPower(UUID gameId, UUID playerId) {
        GamePlayerState playerState = playerStateRepository.findByGameIdAndPlayerId(gameId, playerId)
                .orElseThrow(() -> new IllegalStateException("플레이어 상태를 찾을 수 없습니다"));
        playerState.burnPower();
        playerStateRepository.save(playerState);
        log.info("파워 소각: game={}, player={}, bowl2={}, bowl3={}", gameId, playerId, playerState.getPowerBowl2(), playerState.getPowerBowl3());
    }

    /** 이번 라운드 사용된 파워 액션 + 함대 액션 코드 목록 */
    public List<String> getUsedPowerActionCodes(UUID gameId) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("게임을 찾을 수 없습니다"));
        int round = game.getCurrentRound() != null ? game.getCurrentRound() : 1;

        // 파워 액션
        List<String> codes = new java.util.ArrayList<>(
            powerActionUsageRepository.findByGameIdAndRoundNumber(gameId, round)
                .stream()
                .map(usage -> usage.getPowerActionType().name())
                .toList()
        );

        // 함대 액션 (GameAction에서 FLEET_SHIP_ACTION 타입의 actionData에서 코드 추출)
        List<GameAction> fleetActions = gameActionRepository.findByGameIdAndRoundNumber(gameId, round)
                .stream()
                .filter(a -> a.getActionType() == ActionType.FLEET_SHIP_ACTION)
                .toList();
        for (GameAction fa : fleetActions) {
            String data = fa.getActionData();
            if (data != null && data.contains("\"actionCode\":\"")) {
                // {"actionCode":"REBELLION_TECH",...} 에서 코드 추출
                int start = data.indexOf("\"actionCode\":\"") + 14;
                int end = data.indexOf("\"", start);
                if (end > start) {
                    String actionCode = data.substring(start, end);
                    // FE 코드: FLEET_REBELLION_1 형태로 변환
                    String fleetCode = switch (actionCode) {
                        case "TF_MARS_VP" -> "FLEET_TF_MARS_1";
                        case "TF_MARS_GAIAFORM" -> "FLEET_TF_MARS_2";
                        case "TF_MARS_TERRAFORM" -> "FLEET_TF_MARS_3";
                        case "ECLIPSE_VP" -> "FLEET_ECLIPSE_1";
                        case "ECLIPSE_TECH" -> "FLEET_ECLIPSE_2";
                        case "ECLIPSE_MINE" -> "FLEET_ECLIPSE_3";
                        case "REBELLION_TECH" -> "FLEET_REBELLION_1";
                        case "REBELLION_UPGRADE" -> "FLEET_REBELLION_2";
                        case "REBELLION_CONVERT" -> "FLEET_REBELLION_3";
                        case "TWILIGHT_FED" -> "FLEET_TWILIGHT_1";
                        case "TWILIGHT_UPGRADE" -> "FLEET_TWILIGHT_2";
                        case "TWILIGHT_NAV" -> "FLEET_TWILIGHT_3";
                        default -> actionCode;
                    };
                    codes.add(fleetCode);
                }
            }
        }

        return codes;
    }

    public UsePowerActionResponse usePowerAction(UUID gameId, UsePowerActionRequest request) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("게임을 찾을 수 없습니다"));

        if (!"PLAYING".equals(game.getGamePhase())) {
            return UsePowerActionResponse.fail(gameId, request.powerActionCode(), "PLAYING 페이즈가 아닙니다");
        }

        PowerActionType usageType = toUsageType(request.powerActionCode());
        if (usageType == null) {
            return UsePowerActionResponse.fail(gameId, request.powerActionCode(), "알 수 없는 파워 액션입니다");
        }

        // 라운드당 1회 사용 제한 체크
        if (powerActionUsageRepository.existsByGameIdAndRoundNumberAndPowerActionType(gameId, game.getCurrentRound(), usageType)) {
            return UsePowerActionResponse.fail(gameId, request.powerActionCode(), "이번 라운드에 이미 사용된 파워 액션입니다");
        }

        GamePlayerState playerState = playerStateRepository.findByGameIdAndPlayerId(gameId, request.playerId())
                .orElseThrow(() -> new IllegalStateException("플레이어 상태를 찾을 수 없습니다"));

        PowerActionEffect effect = getEffect(request.powerActionCode());
        if (effect != null) {
            // 기본 파워 액션: 즉시 자원 처리
            // 타클론: FE에서 useBrainstone 플래그 전달
            if (request.useBrainstone() != null && request.useBrainstone()) {
                playerState.setUseBrainstone(true);
            }
            try {
                playerState.spendPower(effect.powerCost());
                if (effect.creditsGain() > 0) playerState.addCredit(effect.creditsGain());
                if (effect.oreGain() > 0) playerState.addOre(effect.oreGain());
                if (effect.knowledgeGain() > 0) playerState.addKnowledge(effect.knowledgeGain());
                if (effect.qicGain() > 0) playerState.addQic(effect.qicGain());
                if (effect.powerTokenGain() > 0) playerState.addPowerToken(effect.powerTokenGain());
            } catch (IllegalStateException e) {
                return UsePowerActionResponse.fail(gameId, request.powerActionCode(), e.getMessage());
            }
            playerStateRepository.save(playerState);
        }
        // 함대 파워 액션(FLEET_*)은 자원 처리 없이 사용 기록만 저장

        // 사용 기록 저장
        powerActionUsageRepository.save(GamePowerActionUsage.builder()
                .gameId(gameId)
                .roundNumber(game.getCurrentRound())
                .powerActionType(usageType)
                .playerId(request.playerId())
                .build());

        log.info("파워 액션: game={}, player={}, action={}", gameId, request.playerId(), request.powerActionCode());

        // 테라포밍 파워 액션은 후속 광산 건설에서 턴을 넘기므로 여기서 턴 넘기지 않음
        boolean isTerraformAction = "PWR_TERRAFORM".equals(request.powerActionCode())
                || "PWR_TERRAFORM_2".equals(request.powerActionCode());
        if (isTerraformAction) {
            return UsePowerActionResponse.success(gameId, request.powerActionCode(), 0);
        }

        String actionData = String.format("{\"powerActionCode\":\"%s\"}", request.powerActionCode());
        ConfirmActionResponse actionResult = actionService.saveActionAndNextTurn(
                gameId, request.playerId(), ActionType.POWER_ACTION, actionData);

        return UsePowerActionResponse.success(gameId, request.powerActionCode(),
                actionResult.nextTurnSeatNo() != null ? actionResult.nextTurnSeatNo() : 0);
    }
}
