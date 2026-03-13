package com.gaiaproject.service;

import com.gaiaproject.domain.entity.artifact.GameArtifactOffer;
import com.gaiaproject.domain.entity.player.GamePlayerArtifact;
import com.gaiaproject.domain.entity.player.GamePlayerState;
import com.gaiaproject.domain.enumtype.artifact.ArtifactType;
import com.gaiaproject.repository.artifact.GameArtifactOfferRepository;
import com.gaiaproject.repository.player.GamePlayerArtifactRepository;
import com.gaiaproject.repository.player.GamePlayerFleetProbeRepository;
import com.gaiaproject.repository.player.GamePlayerStateRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class ArtifactService {

    /* 플레이어 인공물 소유 여부 저장소 */
    private final GamePlayerArtifactRepository artifactRepository;

    /* 플레이어 인공물 저장소 */
    private final GameArtifactOfferRepository artifactOfferRepository;
    private final GamePlayerFleetProbeRepository fleetProbeRepository;
    private final GamePlayerStateRepository playerStateRepository;

    /**
     * 인공물 초기 셋팅
     */
    public void setupArtifactTiles(UUID gameId) {
        // 1. 인공물 타일 랜덤 4개 셋팅
        List<ArtifactType> randomSetup = ArtifactType.getRandomSetup();

        int position = 1;
        for (ArtifactType artifact : randomSetup) {
            GameArtifactOffer offer = GameArtifactOffer.builder()
                    .gameId(gameId)
                    .artifactType(artifact)
                    .position(position++)
                    .build();

            artifactOfferRepository.save(offer);
        }

    }

    /**
     * 인공물 획득 가능 여부 확인
     */
    public boolean canAcquireArtifact(UUID gameId, UUID playerId) {
        // 1. 트와일라잇 함대에 탐사선이 있는지 확인
        boolean hasProbeInTwilight = fleetProbeRepository.existsByGameIdAndPlayerIdAndFleetName(
                gameId, playerId, "TWILIGHT"
        );

        if (!hasProbeInTwilight) {
            return false;
        }

        // 2. 파워 토큰 6개 이상 보유 확인
        GamePlayerState playerState = playerStateRepository
                .findByGameIdAndPlayerId(gameId, playerId)
                .orElseThrow(() -> new IllegalStateException("플레이어 상태를 찾을 수 없습니다"));

        int totalPower = playerState.getPowerBowl1() +
                playerState.getPowerBowl2() +
                playerState.getPowerBowl3();

        return totalPower >= 6;
    }

    /**
     * 인공물 획득
     */
    public void acquireArtifact(UUID gameId, UUID playerId, ArtifactType artifactType) {
        if (!canAcquireArtifact(gameId, playerId)) {
            throw new IllegalStateException("인공물 획득 조건을 만족하지 않습니다");
        }

        // 1. 파워 토큰 6개 영구 제거
        GamePlayerState playerState = playerStateRepository
                .findByGameIdAndPlayerId(gameId, playerId)
                .orElseThrow(() -> new IllegalStateException("플레이어 상태를 찾을 수 없습니다"));

        int removePowerTokenCount = 6;
        removePowerTokens(playerState, removePowerTokenCount);
        playerStateRepository.save(playerState);

        // 2. 인공물 획득
        GamePlayerArtifact artifact = GamePlayerArtifact.builder()
                .gameId(gameId)
                .playerId(playerId)
                .artifactType(artifactType.name())
                .build();

        artifactRepository.save(artifact);

        // 3. 즉시 효과 적용
        if (artifactType.hasImmediateEffect()) {
            applyImmediateEffect(playerState, artifactType);
            playerStateRepository.save(playerState);
        }
    }

    /**
     * 파워 토큰 영구 제거 (Bowl 3 → 2 → 1 순서)
     */
    /**
     * 파워 토큰 영구 제거 (Bowl 1 → 2 → 3 순서)
     * 영구 제거이므로 사용 불가능한 것부터 제거
     */
    private void removePowerTokens(GamePlayerState player, int count) {
        int remaining = count;

        // Bowl 1에서 제거 (가장 먼저)
        int fromBowl1 = Math.min(remaining, player.getPowerBowl1());
        player.addPowerBowl1(-fromBowl1);
        remaining -= fromBowl1;

        // Bowl 2에서 제거
        if (remaining > 0) {
            int fromBowl2 = Math.min(remaining, player.getPowerBowl2());
            player.addPowerBowl2(-fromBowl2);
            remaining -= fromBowl2;
        }

        // Bowl 3에서 제거 (마지막)
        if (remaining > 0) {
            int fromBowl3 = Math.min(remaining, player.getPowerBowl3());
            player.addPowerBowl3(-fromBowl3);
            remaining -= fromBowl3;
        }

        if (remaining > 0) {
            throw new IllegalStateException("파워 토큰이 부족합니다");
        }
    }

    /**
     * 즉시 효과 적용
     */
    private void applyImmediateEffect(GamePlayerState player, ArtifactType artifact) {
        // TODO: 고정 자원 지급 및 동적 VP 계산
        // ResourcesVo reward = artifact.getImmediateReward();
        // player.addCredit(reward.credits());
        // ...
    }

    /**
     * 플레이어가 보유한 인공물 목록
     */
    public List<GamePlayerArtifact> getPlayerArtifacts(UUID gameId, UUID playerId) {
        return artifactRepository.findByGameIdAndPlayerId(gameId, playerId);
    }

    /**
     * 플레이어가 보유한 인공물 개수
     */
    public int getArtifactCount(UUID gameId, UUID playerId) {
        return artifactRepository.countByGameIdAndPlayerId(gameId, playerId);
    }

}