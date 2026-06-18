package com.gaiaproject.service;

import com.gaiaproject.domain.entity.player.GamePlayerState;
import com.gaiaproject.repository.player.GamePlayerStateRepository;
import com.gaiaproject.repository.player.RegisteredPlayerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * 게임 종료 시 외부 전적 사이트(gaia-stats)에 결과를 전송하는 서비스.
 *
 * 호출 시점: PassService에서 6라운드 전원 패스 → calculateFinalScores() 완료 직후
 * 전송 방식: 비동기 스레드에서 HTTP POST (게임 흐름을 블로킹하지 않음)
 * 실패 처리: 전적 전송 실패는 게임 로직에 영향을 주지 않으며 로그만 남김
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StatsApiService {

    private final GamePlayerStateRepository playerStateRepository;
    private final RegisteredPlayerRepository registeredPlayerRepository;

    @Value("${gaia-stats.url}")
    private String statsUrl;

    @Value("${gaia-stats.secret}")
    private String statsSecret;

    private static final RestClient restClient = RestClient.create();

    // ── 요청 DTO ─────────────────────────────────────────────

    record MatchRequest(
            String played_at,
            String memo,
            List<PlayerEntry> players
    ) {}

    record PlayerEntry(
            String name,
            String faction,
            int bid_score,
            int total_score
    ) {}

    // ─────────────────────────────────────────────────────────

    /**
     * 게임 결과를 전적 사이트 API로 전송한다.
     * 참가자 중 테스트 계정(is_real_player = false)이 한 명이라도 있으면 전송을 스킵한다.
     *
     * 점수 변환:
     *   - total_score: 비딩 차감 전 원점수 = victoryPoints(현재 DB값) + bidPenalty
     *   - bid_score:   비딩 패널티 값 그대로
     *   - faction:     FactionType enum명을 소문자로 변환 (ex. TERRANS → terrans)
     */
    public void reportMatchResult(UUID gameId) {
        List<GamePlayerState> players = playerStateRepository.findByGameId(gameId);

        List<String> nicknames = players.stream().map(GamePlayerState::getNickname).toList();

        // 테스트 계정이 포함된 게임은 전적 집계에서 제외
        if (registeredPlayerRepository.existsByNicknameInAndIsRealPlayerFalse(nicknames)) {
            log.info("[STATS] 테스트 계정 포함 게임 — 전적 전송 스킵: gameId={}", gameId);
            return;
        }

        List<PlayerEntry> entries = players.stream()
                .map(ps -> new PlayerEntry(
                        ps.getNickname(),
                        ps.getFactionType().getStatsCode(),
                        ps.getBidPenalty(),
                        // calculateFinalScores()에서 bid 차감이 완료된 상태이므로 더해서 원점수 복원
                        ps.getVictoryPoints() + ps.getBidPenalty()
                ))
                .toList();

        MatchRequest body = new MatchRequest(
                LocalDate.now().toString(),
                "gaiaproject gameId=" + gameId,
                entries
        );

        // 비동기 전송: 응답을 기다리지 않고 게임 종료 흐름을 즉시 반환
        Thread.ofVirtual().start(() -> {
            try {
                restClient
                        .post()
                        .uri(statsUrl)
                        .header("Authorization", "Bearer " + statsSecret)
                        .body(body)
                        .retrieve()
                        .toBodilessEntity();
                log.info("[STATS] 전적 등록 성공: gameId={}", gameId);
            } catch (Exception e) {
                log.warn("[STATS] 전적 등록 실패: gameId={}, error={}", gameId, e.getMessage());
            }
        });
    }
}
