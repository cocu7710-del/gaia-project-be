package com.gaiaproject.service;

import com.gaiaproject.domain.entity.game.Game;
import com.gaiaproject.domain.entity.booster.GameBoosterOffer;
import com.gaiaproject.domain.entity.player.GamePlayerRoundBooster;
import com.gaiaproject.domain.entity.player.GamePlayerState;
import com.gaiaproject.domain.enumtype.booster.BoosterActionType;
import com.gaiaproject.domain.enumtype.booster.RoundBoosterType;
import com.gaiaproject.repository.booster.GameBoosterOfferRepository;
import com.gaiaproject.repository.player.GamePlayerRoundBoosterRepository;
import com.gaiaproject.repository.player.GamePlayerStateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
@RequiredArgsConstructor
public class RoundBoosterService {

    private final GameBoosterOfferRepository boosterOfferRepository;
    private final GamePlayerRoundBoosterRepository playerBoosterRepository;
    private final GamePlayerStateRepository playerStateRepository;

    /**
     * 게임 방 생성 시 이번 게임에서 사용할 라운드 부스터를 랜덤으로 생성한다.
     * - 4인 고정이면 7개 (playerCount + 3)
     * - 중복 없이 선택
     * - 이미 생성된 게임이면 재생성 방지
     */
    public void initializeBoosters(Game game, int playerCount) {
        int boosterCount = playerCount + 3;

        if (boosterOfferRepository.existsByGameId(game.getId())) {
            throw new IllegalStateException(
                    "round boosters already initialized. gameId=" + game.getId()
            );
        }

        List<RoundBoosterType> roundBoosterList = RoundBoosterType.getRandomTiles(boosterCount);

        for (int position = 0; position < boosterCount; position++) {
            // DB에는 enum 이름 그대로 저장: "BOOSTER_1" ...
            String boosterCode = roundBoosterList.get(position).name();
            GameBoosterOffer offer = GameBoosterOffer.create(game, boosterCode, position);
            boosterOfferRepository.save(offer);
        }
    }

    /**
     * 현재 게임 라운드 부스터 반환
     */
    public List<GameBoosterOffer> getBoosters(UUID gameId) {
        return boosterOfferRepository.findByGameIdOrderByPositionAsc(gameId);
    }

    /**
     * 라운드 부스터 선택 (초기 선택)
     * - 초기 선택 순서: 4 → 3 → 2 → 1 (역순)
     *
     * @param gameId      게임 ID
     * @param seatNo      좌석 번호 (1~4)
     * @param playerId    플레이어 ID
     * @param boosterCode 부스터 코드 (예: BOOSTER_1)
     * @return 실패 시 에러 메시지, 성공 시 null
     */
    public String pickBooster(UUID gameId, int seatNo, UUID playerId, String boosterCode) {
        // 1) 부스터 존재 및 선택 가능 여부 확인
        GameBoosterOffer offer = boosterOfferRepository.findByGameIdAndBoosterCode(gameId, boosterCode)
                .orElse(null);

        if (offer == null) {
            return "해당 부스터가 이 게임에 없습니다: " + boosterCode;
        }

        if (!offer.isAvailable()) {
            return "이미 선택된 부스터입니다: " + boosterCode;
        }

        // 2) 해당 플레이어가 이미 부스터를 선택했는지 확인
        if (playerBoosterRepository.findByGameIdAndPlayerId(gameId, playerId).isPresent()) {
            return "이미 부스터를 선택한 플레이어입니다.";
        }

        // 3) 부스터 선택 처리
        offer.pick(seatNo);
        boosterOfferRepository.saveAndFlush(offer);  // 명시적 저장 + 즉시 반영

        // 4) 플레이어 부스터 기록 생성
        RoundBoosterType boosterType = RoundBoosterType.valueOf(boosterCode);
        GamePlayerRoundBooster playerBooster = GamePlayerRoundBooster.builder()
                .gameId(gameId)
                .playerId(playerId)
                .roundBoosterType(boosterType)
                .build();
        playerBoosterRepository.save(playerBooster);

        return null; // 성공
    }

    /**
     * 부스터 액션 사용 (1회, 라운드당)
     * - 현재 소유한 부스터의 액션이 있는지 확인
     * - 이미 사용했으면 에러
     * - 사용 처리 후 액션 타입 반환
     */
    public BoosterActionType useBoosterAction(UUID gameId, UUID playerId) {
        GamePlayerRoundBooster booster = playerBoosterRepository.findByGameIdAndPlayerId(gameId, playerId)
                .orElseThrow(() -> new IllegalStateException("부스터를 찾을 수 없습니다"));

        if (booster.getRoundBoosterType().getActionType() == BoosterActionType.NONE) {
            throw new IllegalStateException("이 부스터에는 액션이 없습니다");
        }

        GamePlayerState state = playerStateRepository.findByGameIdAndPlayerId(gameId, playerId)
                .orElseThrow(() -> new IllegalStateException("플레이어 상태를 찾을 수 없습니다"));

        if (state.isBoosterActionUsed()) {
            throw new IllegalStateException("이미 이번 라운드에 부스터 액션을 사용했습니다");
        }

        state.markBoosterActionUsed();
        playerStateRepository.save(state);

        return booster.getRoundBoosterType().getActionType();
    }

    /**
     * 현재 부스터를 선택해야 할 좌석 번호 반환
     * - 초기 선택 순서: 4 → 3 → 2 → 1
     * - 4명 모두 선택 완료 시 0 반환
     */
    public int getCurrentPickSeatNo(UUID gameId) {
        long pickedCount = boosterOfferRepository.countByGameIdAndPickedBySeatNoIsNotNull(gameId);
        System.out.println("=== [DEBUG] pickedCount: " + pickedCount + " ===");
        if (pickedCount >= 4) {
            return 0; // 모두 선택 완료
        }
        return 4 - (int) pickedCount; // 4, 3, 2, 1 순서
    }
}
