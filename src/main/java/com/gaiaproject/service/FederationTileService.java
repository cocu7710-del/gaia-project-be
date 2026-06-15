package com.gaiaproject.service;

import com.gaiaproject.domain.entity.federation.GameFederationOffer;
import com.gaiaproject.domain.enumtype.federation.FederationActionType;
import com.gaiaproject.domain.enumtype.federation.FederationTileType;
import com.gaiaproject.repository.federation.GameFederationOfferRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class FederationTileService {

    private final GameFederationOfferRepository gameFederationOfferRepository;

    /**
     * 게임에 연방 타일 초기화
     *
     * @param gameId 게임 ID
     */
    public void setupFederationTiles(UUID gameId) {
        // 1. 기본 연방 타일 중 1개를 테라포밍 트랙용으로 랜덤 선택
        List<FederationTileType> basicTiles = FederationTileType.getBasicTiles();
        FederationTileType trackRewardTile = basicTiles.getFirst();

        // 2. 기본 연방 타일 (6종) - 일반 공급처
        for (FederationTileType tile : getBasicFederationTiles()) {
            int quantity = (tile == trackRewardTile) ? 2 : 3;  // 트랙용은 2개만

            gameFederationOfferRepository.save(
                    GameFederationOffer.builder()
                            .gameId(gameId)
                            .federationTileType(tile)
                            .quantity(quantity)
                            .position(null)
                            .build()
            );
        }

        // 3. 테라포밍 트랙에 배치 (위에서 선택한 타일)
        gameFederationOfferRepository.save(
                GameFederationOffer.builder()
                        .gameId(gameId)
                        .federationTileType(trackRewardTile)
                        .quantity(1)
                        .position(0)  // 테라포밍 트랙
                        .build()
        );

        // 4. 확장팩 연방 타일 (8종 중 4종 랜덤) - 잊힌 함대
        List<FederationTileType> expansionTiles = FederationTileType.getRandomExpansionFour();

        for (int i = 0; i < 4; i++) {
            gameFederationOfferRepository.save(
                    GameFederationOffer.builder()
                            .gameId(gameId)
                            .federationTileType(expansionTiles.get(i))
                            .quantity(1)
                            .position(i + 1)  // 잊힌 함대 1~4
                            .build()
            );
        }
    }

    /**
     * 기본 연방 타일 목록
     */
    private List<FederationTileType> getBasicFederationTiles() {
        return Arrays.asList(
                FederationTileType.FED_TILE_1,
                FederationTileType.FED_TILE_2,
                FederationTileType.FED_TILE_3,
                FederationTileType.FED_TILE_4,
                FederationTileType.FED_TILE_5,
                FederationTileType.FED_TILE_6
        );
    }

    /**
     * 확장팩 연방 타일 목록
     */
    private List<FederationTileType> getExpansionFederationTiles() {
        return Arrays.asList(
                FederationTileType.FED_EXP_TILE_1,
                FederationTileType.FED_EXP_TILE_2,
                FederationTileType.FED_EXP_TILE_3,
                FederationTileType.FED_EXP_TILE_4,
                FederationTileType.FED_EXP_TILE_5,
                FederationTileType.FED_EXP_TILE_6,
                FederationTileType.FED_EXP_TILE_7,
                FederationTileType.FED_EXP_TILE_8
        );
    }

    /**
     * 게임의 연방 타일 목록 조회
     */
    public List<GameFederationOffer> getFederationTiles(UUID gameId) {
        return gameFederationOfferRepository.findByGameId(gameId);
    }

    /**
     * 연방 타일 획득 (개수 차감)
     */
    public void acquireFederationTile(UUID gameId, FederationTileType tileType) {
        GameFederationOffer offer = gameFederationOfferRepository.findByGameId(gameId)
                .stream()
                .filter(o -> o.getFederationTileType() == tileType)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("해당 연방 타일을 찾을 수 없습니다"));

        offer.decreaseQuantity();
        gameFederationOfferRepository.save(offer);
    }

    /**
     * 게임의 연방 타일 초기화 (재설정)
     */
    public void resetFederationTiles(UUID gameId) {
        gameFederationOfferRepository.deleteByGameId(gameId);
        setupFederationTiles(gameId);
    }

    /**
     * 연방 타일 수량 차감 (static, 외부 서비스에서 호출)
     * — 일반 supply(position=null)와 잊힌 함대(position>=1)에서 차감.
     *   보드 꼭대기 트랙 보상(position=0)은 제외 — 그건 handleTrackLevel5Entry 가 별도로 차감.
     *   같은 tileType이 일반 supply와 트랙 보상에 동시 존재할 때(트랙용으로 뽑힌 기본 타일)
     *   트랙 보상이 잘못 차감되는 버그 방지.
     */
    public static void acquireTileFromOffer(GameFederationOfferRepository repo, UUID gameId, FederationTileType tileType) {
        GameFederationOffer offer = repo.findByGameId(gameId).stream()
                .filter(o -> o.getFederationTileType() == tileType
                        && o.getQuantity() > 0
                        && (o.getPosition() == null || o.getPosition() != 0))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("해당 연방 타일을 찾을 수 없습니다: " + tileType));
        offer.decreaseQuantity();
        repo.save(offer);
    }
}