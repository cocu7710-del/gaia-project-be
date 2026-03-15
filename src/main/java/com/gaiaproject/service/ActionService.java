package com.gaiaproject.service;

import com.gaiaproject.domain.entity.game.Game;
import com.gaiaproject.domain.entity.game.GameAction;
import com.gaiaproject.domain.entity.game.GameSeat;
import com.gaiaproject.domain.enumtype.action.ActionType;
import com.gaiaproject.dto.response.ConfirmActionResponse;
import com.gaiaproject.repository.game.GameActionRepository;
import com.gaiaproject.repository.game.GamePlayerPassRepository;
import com.gaiaproject.repository.game.GameRepository;
import com.gaiaproject.repository.game.GameSeatRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * 게임 액션 관리 서비스
 * - FE에서 확정된 액션만 DB에 저장 (기록용)
 * - 액션 확정 시 자동 턴 넘김
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class ActionService {

    private final GameActionRepository actionRepository;
    private final GamePlayerPassRepository passRepository;
    private final GameRepository gameRepository;
    private final GameSeatRepository seatRepository;
    private final GameWebSocketService webSocketService;

    /**
     * 액션 저장 + 자동 턴 넘김
     * FE에서 확정 버튼을 누른 후 호출
     */
    public ConfirmActionResponse saveActionAndNextTurn(UUID gameId, UUID playerId,
                                                        ActionType actionType, String actionData) {
        try {
            Game game = gameRepository.findById(gameId)
                    .orElseThrow(() -> new IllegalArgumentException("게임을 찾을 수 없습니다"));

            // 현재 라운드와 턴 시퀀스 조회
            int currentRound = game.getCurrentRound() != null ? game.getCurrentRound() : 1;
            int turnSequence = calculateCurrentTurnSequence(gameId, currentRound);

            // 액션 저장 (기록용)
            GameAction action = GameAction.builder()
                    .gameId(gameId)
                    .playerId(playerId)
                    .roundNumber(currentRound)
                    .turnSequence(turnSequence)
                    .actionType(actionType)
                    .actionData(actionData)
                    .build();

            action = actionRepository.save(action);

            log.info("액션 저장: actionId={}, type={}, player={}", action.getId(), actionType, playerId);

            // 다음 턴 계산
            int nextSeatNo = calculateNextTurnSeatNo(game);
            boolean roundEnded = (nextSeatNo == 0);

            if (roundEnded) {
                // 라운드 종료 처리
                endRoundAndStartNext(game);
                webSocketService.broadcastRoundStarted(gameId, game.getCurrentRound());
            } else {
                // 턴 넘김
                game.nextTurn(nextSeatNo);
                gameRepository.save(game);
                webSocketService.broadcastTurnChanged(gameId, nextSeatNo);
            }

            return ConfirmActionResponse.success(gameId, action.getId(), nextSeatNo, roundEnded);

        } catch (Exception e) {
            log.error("액션 저장 실패: gameId={}, playerId={}", gameId, playerId, e);
            return ConfirmActionResponse.fail(gameId, null, "액션 저장 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 액션 저장만 (턴 진행 없음) — 파워 리치 결정 후 턴 진행 시 사용
     */
    public void saveActionOnly(UUID gameId, UUID playerId, ActionType actionType, String actionData) {
        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("게임을 찾을 수 없습니다"));
        int currentRound = game.getCurrentRound() != null ? game.getCurrentRound() : 1;
        int turnSequence = calculateCurrentTurnSequence(gameId, currentRound);

        GameAction action = GameAction.builder()
                .gameId(gameId).playerId(playerId)
                .roundNumber(currentRound).turnSequence(turnSequence)
                .actionType(actionType).actionData(actionData)
                .build();
        actionRepository.save(action);
        log.info("액션 저장(리치 대기): actionType={}, player={}", actionType, playerId);
    }

    /**
     * 턴 진행 + 브로드캐스트 — 파워 리치 해소 완료 후 호출
     */
    public void advanceTurnAndBroadcast(Game game) {
        int nextSeatNo = calculateNextTurnSeatNo(game);
        boolean roundEnded = (nextSeatNo == 0);

        if (roundEnded) {
            endRoundAndStartNext(game);
            webSocketService.broadcastRoundStarted(game.getId(), game.getCurrentRound());
        } else {
            game.nextTurn(nextSeatNo);
            gameRepository.save(game);
            webSocketService.broadcastTurnChanged(game.getId(), nextSeatNo);
        }
    }

    /**
     * 다음 턴 좌석 번호 계산
     */
    private int calculateNextTurnSeatNo(Game game) {
        UUID gameId = game.getId();
        int currentRound = game.getCurrentRound();
        int currentSeatNo = game.getCurrentTurnSeatNo();

        // 모든 좌석 조회
        List<GameSeat> seats = seatRepository.findByGameIdOrderBySeatNo(gameId);
        int maxSeatNo = seats.size();

        // 패스하지 않은 플레이어 찾기 (순환)
        for (int i = 1; i <= maxSeatNo; i++) {
            int nextSeatNo = (currentSeatNo % maxSeatNo) + 1;
            currentSeatNo = nextSeatNo;

            // 해당 좌석의 플레이어가 패스했는지 확인
            GameSeat seat = seats.stream()
                    .filter(s -> s.getSeatNo() == nextSeatNo)
                    .findFirst()
                    .orElse(null);

            if (seat != null && seat.getPlayerId() != null) {
                boolean hasPassed = passRepository.existsByGameIdAndPlayerIdAndRoundNumber(
                        gameId, seat.getPlayerId(), currentRound);

                if (!hasPassed) {
                    return nextSeatNo;
                }
            }
        }

        // 모든 플레이어가 패스한 경우
        return 0;
    }

    /**
     * 현재 턴 시퀀스 계산
     */
    private int calculateCurrentTurnSequence(UUID gameId, int roundNumber) {
        List<GameAction> actions = actionRepository.findByGameIdAndRoundNumber(gameId, roundNumber);
        return actions.size() + 1;
    }

    /**
     * 라운드 종료 및 다음 라운드 시작
     */
    private void endRoundAndStartNext(Game game) {
        log.info("라운드 {} 종료, 다음 라운드 시작", game.getCurrentRound());

        // TODO: 라운드 종료 처리 (수입 배분, 부스터 반환 등)

        // 다음 라운드로 이동
        game.nextRound();
        gameRepository.save(game);

        log.info("라운드 {} 시작", game.getCurrentRound());
    }
}
