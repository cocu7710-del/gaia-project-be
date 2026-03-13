package com.gaiaproject.service;

import com.gaiaproject.domain.entity.game.GameSeat;
import com.gaiaproject.domain.enumtype.player.FactionType;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 초기 광산 배치 순서 계산 서비스
 *
 * 규칙:
 * 1. 기본 종족: 1→2→3→4→4→3→2→1 (snake draft)
 * 2. 제노스(XENOS): 1번 단계 완료 후 광산 1개 추가 배치
 * 3. 확장 종족: 기본 종족 배치 완료 후, 턴 순서대로 1개씩 배치
 * 4. 하이브(IVITS): 항상 마지막에 배치
 */
@Slf4j
@Service
public class MineSetupOrderService {

    @Getter
    public static class SetupTurn {
        private final int seatNo;
        private final FactionType faction;
        private final String phase; // "FIRST", "SECOND", "XENOS_EXTRA", "EXPANSION", "IVITS"

        public SetupTurn(int seatNo, FactionType faction, String phase) {
            this.seatNo = seatNo;
            this.faction = faction;
            this.phase = phase;
        }

        @Override
        public String toString() {
            return String.format("Seat#%d(%s, %s)", seatNo, faction.getDisplayNameKo(), phase);
        }
    }

    /**
     * 전체 배치 순서 계산
     *
     * @param seats 좌석 목록 (seatNo 1~4)
     * @return 배치 순서 리스트 (인덱스 0부터)
     */
    public List<SetupTurn> calculateSetupOrder(List<GameSeat> seats) {
        if (seats.size() != 4) {
            throw new IllegalArgumentException("좌석은 정확히 4개여야 합니다");
        }

        List<SetupTurn> order = new ArrayList<>();

        // 좌석 번호 순으로 정렬
        List<GameSeat> sortedSeats = seats.stream()
                .sorted(Comparator.comparingInt(GameSeat::getSeatNo))
                .toList();

        // 하이브, 제노스, 확장 종족 분류
        GameSeat ivitsSeat = null;
        GameSeat xenosSeat = null;
        List<GameSeat> basicSeats = new ArrayList<>();
        List<GameSeat> expansionSeats = new ArrayList<>();

        for (GameSeat seat : sortedSeats) {
            FactionType faction = seat.getFactionType();
            if (faction.isIvits()) {
                ivitsSeat = seat;
            } else if (faction.isXenos()) {
                xenosSeat = seat;
            } else if (faction.isExpansionFaction()) {
                expansionSeats.add(seat);
            } else {
                basicSeats.add(seat);
            }
        }

        // === 1단계: 기본 종족 첫 번째 배치 (제노스 포함) ===
        List<GameSeat> firstRoundSeats = new ArrayList<>(basicSeats);
        if (xenosSeat != null) {
            firstRoundSeats.add(xenosSeat);
        }
        firstRoundSeats.sort(Comparator.comparingInt(GameSeat::getSeatNo));

        for (GameSeat seat : firstRoundSeats) {
            order.add(new SetupTurn(seat.getSeatNo(), seat.getFactionType(), "FIRST"));
        }

        // === 2단계: 기본 종족 두 번째 배치 (역순, 제노스 포함) ===
        List<GameSeat> secondRoundSeats = new ArrayList<>(firstRoundSeats);
        Collections.reverse(secondRoundSeats);

        for (GameSeat seat : secondRoundSeats) {
            order.add(new SetupTurn(seat.getSeatNo(), seat.getFactionType(), "SECOND"));
        }

        // === 제노스 추가 광산 (2단계 이후) ===
        if (xenosSeat != null) {
            order.add(new SetupTurn(xenosSeat.getSeatNo(), xenosSeat.getFactionType(), "XENOS_EXTRA"));
        }

        // === 확장 종족 배치 (턴 순서대로) ===
        expansionSeats.sort(Comparator.comparingInt(GameSeat::getSeatNo));
        for (GameSeat seat : expansionSeats) {
            order.add(new SetupTurn(seat.getSeatNo(), seat.getFactionType(), "EXPANSION"));
        }

        // === 하이브 배치 (항상 마지막) ===
        if (ivitsSeat != null) {
            order.add(new SetupTurn(ivitsSeat.getSeatNo(), ivitsSeat.getFactionType(), "IVITS"));
        }

        log.info("=== 초기 광산 배치 순서 ===");
        for (int i = 0; i < order.size(); i++) {
            log.info("  [{}] {}", i, order.get(i));
        }

        return order;
    }

    /**
     * 현재 인덱스로 배치할 좌석 번호 조회
     */
    public int getSeatNoAtIndex(List<SetupTurn> order, int index) {
        if (index < 0 || index >= order.size()) {
            throw new IllegalArgumentException("Invalid setup index: " + index);
        }
        return order.get(index).getSeatNo();
    }

    /**
     * 현재 페이즈 문자열 생성 (프론트엔드 표시용)
     */
    public String getPhaseString(List<SetupTurn> order, int index) {
        if (index < 0 || index >= order.size()) {
            return "BOOSTER_SELECTION";
        }
        SetupTurn turn = order.get(index);
        return switch (turn.getPhase()) {
            case "FIRST" -> "SETUP_MINE_FIRST";
            case "SECOND" -> "SETUP_MINE_SECOND";
            case "XENOS_EXTRA" -> "SETUP_MINE_XENOS";
            case "EXPANSION" -> "SETUP_MINE_EXPANSION";
            case "IVITS" -> "SETUP_MINE_IVITS";
            default -> "UNKNOWN";
        };
    }
}
