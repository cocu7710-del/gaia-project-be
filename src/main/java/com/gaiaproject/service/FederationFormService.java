package com.gaiaproject.service;

import com.gaiaproject.domain.entity.building.GameBuilding;
import com.gaiaproject.domain.entity.map.GameHex;
import com.gaiaproject.domain.entity.federation.GameFederationBuilding;
import com.gaiaproject.domain.entity.federation.GameFederationGroup;
import com.gaiaproject.domain.entity.federation.GameFederationTokenHex;
import com.gaiaproject.domain.entity.game.Game;
import com.gaiaproject.domain.entity.player.GamePlayerFederationToken;
import com.gaiaproject.domain.entity.player.GamePlayerState;
import com.gaiaproject.domain.enumtype.building.BuildingType;
import com.gaiaproject.domain.enumtype.federation.FederationActionType;
import com.gaiaproject.domain.enumtype.federation.FederationTileType;
import com.gaiaproject.domain.enumtype.player.FactionType;
import com.gaiaproject.dto.request.FormFederationRequest;
import com.gaiaproject.dto.response.ConfirmActionResponse;
import com.gaiaproject.dto.response.FormFederationResponse;
import com.gaiaproject.repository.building.GameBuildingRepository;
import com.gaiaproject.repository.federation.*;
import com.gaiaproject.repository.game.GameRepository;
import com.gaiaproject.repository.player.GamePlayerFederationTokenRepository;
import com.gaiaproject.repository.player.GamePlayerStateRepository;
import com.gaiaproject.repository.tech.GamePlayerTechTileRepository;
import com.gaiaproject.util.HexUtil;
import com.gaiaproject.domain.enumtype.action.ActionType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class FederationFormService {

    private final GameRepository gameRepository;
    private final GameBuildingRepository buildingRepository;
    private final GamePlayerStateRepository playerStateRepository;
    private final GameFederationGroupRepository federationGroupRepository;
    private final GameFederationBuildingRepository federationBuildingRepository;
    private final GameFederationTokenHexRepository federationTokenHexRepository;
    private final GameFederationOfferRepository federationOfferRepository;
    private final GamePlayerFederationTokenRepository playerFederationTokenRepository;
    private final ActionService actionService;
    private final GameWebSocketService webSocketService;
    private final GamePlayerTechTileRepository playerTechTileRepository;
    private final RoundScoringService roundScoringService;
    private final com.gaiaproject.repository.map.GameHexRepository hexRepository;
    private final VpLogService vpLogService;
    private final TechTileService techTileService;

    /**
     * 건물 선택 검증 (토큰 배치 전). 파워 합계 + 기존 연방 중복 체크 + 최소 토큰 수 반환.
     */
    public Map<String, Object> validateBuildingSelection(UUID gameId, FormFederationRequest request) {
        UUID playerId = request.playerId();
        GamePlayerState ps = playerStateRepository.findByGameIdAndPlayerId(gameId, playerId)
                .orElseThrow(() -> new IllegalStateException("플레이어 상태를 찾을 수 없습니다"));

        boolean isIvits = ps.getFactionType() == FactionType.IVITS;
        List<int[]> buildingHexes = request.buildingHexes() != null ? request.buildingHexes() : List.of();

        if (buildingHexes.isEmpty()) {
            return Map.of("success", false, "message", "건물을 선택해야 합니다");
        }

        // 건물 검증
        List<GameBuilding> myBuildings = buildingRepository.findByGameIdAndPlayerId(gameId, playerId);
        Map<String, GameBuilding> myBuildingMap = myBuildings.stream()
                .collect(Collectors.toMap(b -> b.getHexQ() + "," + b.getHexR(), b -> b, (a, c) -> a));

        int totalPower = 0;
        for (int[] hex : buildingHexes) {
            String key = hex[0] + "," + hex[1];
            GameBuilding b = myBuildingMap.get(key);
            if (b == null) return Map.of("success", false, "message", "내 건물이 아닙니다: (" + hex[0] + "," + hex[1] + ")");
            // 란티다 기생 광산은 항상 광산 파워(1)로 취급
            totalPower += b.isLantidsMine() ? 1 : buildingPowerValue(b.getBuildingType(), gameId, playerId, b.isHasRing());
        }
        // 매안 PI: 본인 행성(TITANIUM) 건물 파워값 +1
        if (ps.getFactionType() == FactionType.BESCODS && ps.getStockPlanetaryInstitute() == 0) {
            for (int[] hex : buildingHexes) {
                var hexOpt = hexRepository.findByGameIdAndHexQAndHexR(gameId, hex[0], hex[1]);
                if (hexOpt.isPresent() && hexOpt.get().getPlanetType() == com.gaiaproject.domain.enumtype.player.PlanetType.TITANIUM) {
                    totalPower += 1;
                }
            }
        }

        // 기존 연방 중복 체크 (하이브 제외)
        if (!isIvits) {
            Set<String> usedFedHexes = getUsedFederationHexes(gameId, playerId);
            for (int[] hex : buildingHexes) {
                if (usedFedHexes.contains(hex[0] + "," + hex[1])) {
                    return Map.of("success", false, "message", "이미 연방에 사용된 건물이 포함되어 있습니다");
                }
            }
        }

        // 파워 체크 (제노스 PI: 6, 기본: 7)
        boolean isXenosPi = ps.getFactionType() == FactionType.XENOS && ps.getStockPlanetaryInstitute() == 0;
        int requiredPower = isXenosPi ? 6 : 7;
        if (isIvits) {
            // 카운터 기반: 다음 연방 목표 = (형성횟수+1)*7
            int fedCount = ps.getFederationCount();
            requiredPower = (fedCount + 1) * 7;
            // totalPower = 선택한 건물 + 기존 연방 건물 전체 합산
            List<GameFederationGroup> existingGroups = federationGroupRepository.findByGameIdAndPlayerId(gameId, playerId);
            if (!existingGroups.isEmpty()) {
                Set<String> selectedSet = new HashSet<>();
                for (int[] hex : buildingHexes) selectedSet.add(hex[0] + "," + hex[1]);
                for (GameFederationBuilding fb : federationBuildingRepository.findByFederationGroupId(existingGroups.get(0).getId())) {
                    String key = fb.getHexQ() + "," + fb.getHexR();
                    if (!selectedSet.contains(key)) {
                        GameBuilding b = buildingRepository.findFirstByGameIdAndHexQAndHexRAndIsLantidsMine(gameId, fb.getHexQ(), fb.getHexR(), false).orElse(null);
                        if (b != null) totalPower += buildingPowerValue(b.getBuildingType());
                    }
                }
            }
        }

        if (totalPower < requiredPower) {
            return Map.of("success", false, "message", "파워가 부족합니다 (현재: " + totalPower + ", 필요: " + requiredPower + ")");
        }

        // 최소 토큰 수 계산: BFS 실제 경로 (장애물 우회)
        Set<String> buildingSet = new HashSet<>();
        for (int[] h : buildingHexes) buildingSet.add(h[0] + "," + h[1]);
        // 하이브: 기존 연방 건물/토큰도 연결된 노드로 포함
        if (isIvits) {
            List<GameFederationGroup> existingGroups = federationGroupRepository.findByGameIdAndPlayerId(gameId, playerId);
            if (!existingGroups.isEmpty()) {
                for (GameFederationBuilding fb : federationBuildingRepository.findByFederationGroupId(existingGroups.get(0).getId())) {
                    buildingSet.add(fb.getHexQ() + "," + fb.getHexR());
                }
                for (var ft : federationTokenHexRepository.findByFederationGroupId(existingGroups.get(0).getId())) {
                    buildingSet.add(ft.getHexQ() + "," + ft.getHexR());
                }
            }
        }
        int groups = countConnectedGroups(buildingSet);

        // 토큰 배치 가능 헥스 (EMPTY + 맵 존재 + 연방 인접 아님)
        Set<String> allowedHexes = buildAllowedHexes(gameId, playerId, isIvits);
        // 하이브: 전체 buildingSet 좌표를 buildingHexes로 사용
        List<int[]> effectiveBuildingHexes = isIvits
                ? buildingSet.stream().map(s -> { String[] p = s.split(","); return new int[]{Integer.parseInt(p[0]), Integer.parseInt(p[1])}; }).toList()
                : buildingHexes;
        log.info("[FEDERATION_VALIDATE] allowedHexes count={}, buildings={}", allowedHexes.size(), effectiveBuildingHexes.size());
        int minTokens = calcMinTokensToConnect(effectiveBuildingHexes, gameId, allowedHexes, buildingSet);
        log.info("[FEDERATION_VALIDATE] minTokens={}", minTokens);

        return Map.of("success", true, "totalPower", totalPower, "minTokens", minTokens, "groups", groups);
    }

    /**
     * 선택한 건물들을 모두 연결하는 최소 토큰 수 계산 (Steiner Tree 근사)
     * 모든 연결 순서를 시도하여 경로 공유를 반영한 실제 최소 토큰 수를 반환.
     * 기존 MST 방식은 경로 겹침을 고려하지 못해 과다 계산되는 버그가 있었음.
     */
    private int calcMinTokensToConnect(List<int[]> buildingHexes) {
        return calcMinTokensToConnect(buildingHexes, null, Set.of(), Set.of());
    }

    private int calcMinTokensToConnect(List<int[]> buildingHexes, UUID gameId,
                                         Set<String> allowedHexes, Set<String> buildingHexSet) {
        if (buildingHexes.size() <= 1) return 0;

        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1},{1,-1},{-1,1}};

        // 1. 인접 건물끼리 묶어 연결 그룹 찾기
        List<Set<String>> groups = new ArrayList<>();
        Set<String> assigned = new HashSet<>();
        for (int[] h : buildingHexes) {
            String key = h[0] + "," + h[1];
            if (assigned.contains(key)) continue;
            Set<String> group = new HashSet<>();
            Queue<String> q = new LinkedList<>();
            q.add(key);
            assigned.add(key);
            group.add(key);
            while (!q.isEmpty()) {
                String cur = q.poll();
                String[] parts = cur.split(",");
                int cq = Integer.parseInt(parts[0]);
                int cr = Integer.parseInt(parts[1]);
                for (int[] d : dirs) {
                    String nb = (cq + d[0]) + "," + (cr + d[1]);
                    if (!assigned.contains(nb) && buildingHexSet.contains(nb)) {
                        assigned.add(nb);
                        group.add(nb);
                        q.add(nb);
                    }
                }
            }
            groups.add(group);
        }

        int n = groups.size();
        if (n <= 1) return 0;

        // 2. 모든 연결 순서 시도 (그룹 0 고정, 나머지 순열) → 최소 토큰 수
        int[] indices = new int[n - 1];
        for (int i = 0; i < n - 1; i++) indices[i] = i + 1;

        int minTokens = Integer.MAX_VALUE;
        do {
            int tokens = simulateSteinerConnection(groups, indices, allowedHexes, buildingHexSet);
            minTokens = Math.min(minTokens, tokens);
        } while (nextPermutation(indices));

        log.info("[FEDERATION] Steiner minTokens={}, groups={}", minTokens, n);
        return minTokens;
    }

    /**
     * 주어진 연결 순서(order)로 그룹을 하나씩 연결하며 실제 사용한 고유 토큰 헥스 수 반환.
     * 이전에 배치한 토큰을 후속 경로에서 재활용하므로 공유 경로가 자연스럽게 반영됨.
     */
    private int simulateSteinerConnection(List<Set<String>> groups, int[] order,
                                            Set<String> allowedHexes, Set<String> buildingHexSet) {
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1},{1,-1},{-1,1}};
        Set<String> connected = new HashSet<>(groups.get(0));
        Set<String> placedTokens = new HashSet<>();
        Set<Integer> connectedGroupIds = new HashSet<>();
        connectedGroupIds.add(0);

        for (int idx : order) {
            if (connectedGroupIds.contains(idx)) continue;

            // 0-1 BFS: connected + placedTokens → target group
            List<String> path = bfsSteinerPath(connected, placedTokens, groups.get(idx),
                    allowedHexes, buildingHexSet, dirs);
            if (path == null) return Integer.MAX_VALUE;

            // 경로 상의 EMPTY 헥스를 토큰으로 배치 (건물 헥스는 제외)
            for (String hex : path) {
                if (allowedHexes.contains(hex) && !connected.contains(hex) && !buildingHexSet.contains(hex)) {
                    placedTokens.add(hex);
                }
            }

            // 도달한 그룹 + 경로 상 건물 그룹 모두 connected에 추가
            connected.addAll(groups.get(idx));
            connectedGroupIds.add(idx);
            for (String hex : path) {
                if (buildingHexSet.contains(hex)) {
                    for (int g = 0; g < groups.size(); g++) {
                        if (!connectedGroupIds.contains(g) && groups.get(g).contains(hex)) {
                            connectedGroupIds.add(g);
                            connected.addAll(groups.get(g));
                        }
                    }
                }
            }
        }

        return placedTokens.size();
    }

    /**
     * 0-1 BFS: connected + placedTokens에서 targetGroup까지의 최단 경로 반환.
     * 건물 헥스(비용 0)와 EMPTY 토큰 헥스(비용 1)를 구분하여 최소 토큰 경로를 찾음.
     */
    private List<String> bfsSteinerPath(Set<String> connected, Set<String> placedTokens,
                                          Set<String> targetGroup, Set<String> allowedHexes,
                                          Set<String> buildingHexSet, int[][] dirs) {
        Map<String, String> parent = new HashMap<>();
        Map<String, Integer> dist = new HashMap<>();
        Deque<String> deque = new ArrayDeque<>();

        for (String hex : connected) {
            dist.put(hex, 0);
            deque.addFirst(hex);
        }
        for (String hex : placedTokens) {
            if (!dist.containsKey(hex)) {
                dist.put(hex, 0);
                deque.addFirst(hex);
            }
        }

        while (!deque.isEmpty()) {
            String cur = deque.pollFirst();
            int curDist = dist.get(cur);
            String[] parts = cur.split(",");
            int q = Integer.parseInt(parts[0]);
            int r = Integer.parseInt(parts[1]);

            for (int[] d : dirs) {
                String nb = (q + d[0]) + "," + (r + d[1]);

                // 타겟 그룹 도착 → 경로 역추적
                if (targetGroup.contains(nb)) {
                    parent.put(nb, cur);
                    List<String> path = new ArrayList<>();
                    String trace = nb;
                    while (trace != null && !connected.contains(trace) && !placedTokens.contains(trace)) {
                        path.add(trace);
                        trace = parent.get(trace);
                    }
                    return path;
                }

                int newDist;
                boolean zeroCost;
                if (connected.contains(nb) || placedTokens.contains(nb) || buildingHexSet.contains(nb)) {
                    newDist = curDist;
                    zeroCost = true;
                } else if (allowedHexes.contains(nb)) {
                    newDist = curDist + 1;
                    zeroCost = false;
                } else {
                    continue;
                }

                if (dist.containsKey(nb) && dist.get(nb) <= newDist) continue;
                dist.put(nb, newDist);
                parent.put(nb, cur);
                if (zeroCost) deque.addFirst(nb); else deque.addLast(nb);
            }
        }

        return null;
    }

    /** 다음 사전순 순열 생성 (in-place). 마지막 순열이면 false 반환 */
    private boolean nextPermutation(int[] arr) {
        int i = arr.length - 2;
        while (i >= 0 && arr[i] >= arr[i + 1]) i--;
        if (i < 0) return false;
        int j = arr.length - 1;
        while (arr[j] <= arr[i]) j--;
        int tmp = arr[i]; arr[i] = arr[j]; arr[j] = tmp;
        for (int left = i + 1, right = arr.length - 1; left < right; left++, right--) {
            tmp = arr[left]; arr[left] = arr[right]; arr[right] = tmp;
        }
        return true;
    }

    /**
     * 토큰 배치 가능 헥스 구성 (EMPTY + 맵에 존재 + 자기 기존 연방 인접 아님)
     */
    private Set<String> buildAllowedHexes(UUID gameId, UUID playerId, boolean isIvits) {
        Set<String> allowed = new HashSet<>();
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1},{1,-1},{-1,1}};

        // 맵에 존재하는 EMPTY 헥스 허용 (함대 헥스 제외)
        List<GameHex> allHexes = hexRepository.findByGameId(gameId);
        for (GameHex hex : allHexes) {
            if ("EMPTY".equals(hex.getPlanetType().name())
                    && (hex.getSectorId() == null || !hex.getSectorId().startsWith("FORGOTTEN_FLEET_"))) {
                allowed.add(hex.getHexQ() + "," + hex.getHexR());
            }
        }
        // 내 건물이 있는 헥스도 경유 가능 (선택하지 않은 건물 포함)
        List<GameBuilding> myBuildings = buildingRepository.findByGameIdAndPlayerId(gameId, playerId);
        for (GameBuilding b : myBuildings) {
            allowed.add(b.getHexQ() + "," + b.getHexR());
        }

        // 자기 기존 연방 헥스 + 인접에는 토큰 배치/경유 불가 (하이브 제외)
        if (!isIvits) {
            Set<String> fedHexes = getUsedFederationHexes(gameId, playerId);
            allowed.removeAll(fedHexes);
            for (String fedHex : fedHexes) {
                String[] parts = fedHex.split(",");
                int q = Integer.parseInt(parts[0]);
                int r = Integer.parseInt(parts[1]);
                for (int[] d : dirs) {
                    allowed.remove((q + d[0]) + "," + (r + d[1]));
                }
            }
            // 기존 연방에 사용되지 않은 내 건물만 경유 가능
            for (GameBuilding b : myBuildings) {
                String key = b.getHexQ() + "," + b.getHexR();
                if (!fedHexes.contains(key)) {
                    allowed.add(key);
                }
            }
        }

        return allowed;
    }

    /** BFS로 연결된 그룹 수 계산 */
    private int countConnectedGroups(Set<String> hexes) {
        if (hexes.isEmpty()) return 0;
        Set<String> visited = new HashSet<>();
        int groups = 0;
        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1},{1,-1},{-1,1}};

        for (String hex : hexes) {
            if (visited.contains(hex)) continue;
            groups++;
            Queue<String> queue = new LinkedList<>();
            queue.add(hex);
            visited.add(hex);
            while (!queue.isEmpty()) {
                String cur = queue.poll();
                String[] parts = cur.split(",");
                int q = Integer.parseInt(parts[0]);
                int r = Integer.parseInt(parts[1]);
                for (int[] d : dirs) {
                    String neighbor = (q + d[0]) + "," + (r + d[1]);
                    if (hexes.contains(neighbor) && !visited.contains(neighbor)) {
                        visited.add(neighbor);
                        queue.add(neighbor);
                    }
                }
            }
        }
        return groups;
    }

    /**
     * 연방 배치 조건 체크 (타일 선택 전). DB 변경 없이 검증만 수행.
     */
    public FormFederationResponse validateFederation(UUID gameId, FormFederationRequest request) {
        UUID playerId = request.playerId();
        GamePlayerState ps = playerStateRepository.findByGameIdAndPlayerId(gameId, playerId)
                .orElseThrow(() -> new IllegalStateException("플레이어 상태를 찾을 수 없습니다"));

        boolean isIvits = ps.getFactionType() == FactionType.IVITS;
        List<int[]> tokenHexes = request.tokenHexes() != null ? request.tokenHexes() : List.of();

        List<GameBuilding> myBuildings = buildingRepository.findByGameIdAndPlayerId(gameId, playerId);
        Map<String, GameBuilding> myBuildingMap = myBuildings.stream()
                .collect(Collectors.toMap(b -> b.getHexQ() + "," + b.getHexR(), b -> b, (a, c) -> a));

        // 이미 연방된 헥스 (일반 종족: 제외, 하이브: 포함 가능)
        Set<String> usedFedHexes = isIvits ? Set.of() : getUsedFederationHexes(gameId, playerId);

        // 토큰이 이미 연방된 위치 또는 연방 인접에 있는지 체크
        int[][] adjDirs = {{1,0},{-1,0},{0,1},{0,-1},{1,-1},{-1,1}};
        Set<String> fedBlockedHexes = new HashSet<>(usedFedHexes);
        for (String fedHex : usedFedHexes) {
            String[] parts = fedHex.split(",");
            int fq = Integer.parseInt(parts[0]);
            int fr = Integer.parseInt(parts[1]);
            for (int[] d : adjDirs) fedBlockedHexes.add((fq + d[0]) + "," + (fr + d[1]));
        }
        for (int[] hex : tokenHexes) {
            if (fedBlockedHexes.contains(hex[0] + "," + hex[1])) {
                return FormFederationResponse.fail(gameId, "기존 연방 또는 인접 위치에 토큰을 놓을 수 없습니다");
            }
        }

        // 토큰이 있으면 토큰 기반 BFS, 없으면 사용자 선택 건물 직접 사용
        List<int[]> buildingHexes;
        if (tokenHexes.isEmpty()) {
            // 토큰 없이 건물만으로 연방 (인접한 건물들)
            buildingHexes = request.buildingHexes() != null ? request.buildingHexes() : List.of();
            if (buildingHexes.isEmpty()) {
                return FormFederationResponse.fail(gameId, "연방에 포함할 건물을 선택해야 합니다");
            }
            // 선택된 건물이 실제 내 건물인지 검증
            for (int[] hex : buildingHexes) {
                String key = hex[0] + "," + hex[1];
                if (!myBuildingMap.containsKey(key) || usedFedHexes.contains(key)) {
                    return FormFederationResponse.fail(gameId, "유효하지 않은 건물이 포함되어 있습니다: (" + hex[0] + "," + hex[1] + ")");
                }
            }
        } else {
            buildingHexes = findConnectedBuildings(tokenHexes, myBuildingMap, usedFedHexes);
            if (buildingHexes.isEmpty()) {
                return FormFederationResponse.fail(gameId, "토큰에 연결된 건물이 없습니다");
            }
        }

        List<GameBuilding> selectedBuildings = buildingHexes.stream()
                .map(h -> myBuildingMap.get(h[0] + "," + h[1])).toList();

        // 연결성
        Set<String> allHexes = new HashSet<>();
        for (int[] h : buildingHexes) allHexes.add(h[0] + "," + h[1]);
        for (int[] h : tokenHexes) allHexes.add(h[0] + "," + h[1]);
        // 하이브: 기존 연방 건물/토큰도 연결 대상에 포함
        if (isIvits) {
            List<GameFederationGroup> existingFedGroups = federationGroupRepository.findByGameIdAndPlayerId(gameId, playerId);
            if (!existingFedGroups.isEmpty()) {
                GameFederationGroup eg = existingFedGroups.get(0);
                federationBuildingRepository.findByFederationGroupId(eg.getId())
                        .forEach(b -> allHexes.add(b.getHexQ() + "," + b.getHexR()));
                federationTokenHexRepository.findByFederationGroupId(eg.getId())
                        .forEach(t -> allHexes.add(t.getHexQ() + "," + t.getHexR()));
            }
        }
        if (!isConnected(allHexes)) {
            return FormFederationResponse.fail(gameId, "건물과 토큰이 연결되어 있지 않습니다");
        }

        // 파워 값 (BASIC_TILE_9 반영)
        int totalPowerValue = selectedBuildings.stream().mapToInt(b -> b.isLantidsMine() ? 1 : buildingPowerValue(b.getBuildingType(), gameId, playerId, b.isHasRing())).sum();
        // 매안 PI: 본인 행성(TITANIUM) 건물 파워값 +1
        if (ps.getFactionType() == FactionType.BESCODS && ps.getStockPlanetaryInstitute() == 0) {
            for (GameBuilding b : selectedBuildings) {
                var hexOpt = hexRepository.findByGameIdAndHexQAndHexR(gameId, b.getHexQ(), b.getHexR());
                if (hexOpt.isPresent() && hexOpt.get().getPlanetType() == com.gaiaproject.domain.enumtype.player.PlanetType.TITANIUM) {
                    totalPowerValue += 1;
                }
            }
        }

        // 제노스 PI: 연방 필요 파워 6
        boolean isXenosPi = ps.getFactionType() == FactionType.XENOS && ps.getStockPlanetaryInstitute() == 0;
        int fedRequiredPower = isXenosPi ? 6 : 7;

        if (isIvits) {
            // 하이브: 기존 연방 파워 합산 + fedCount 기반 목표 체크
            List<GameFederationGroup> existingGroups = federationGroupRepository.findByGameIdAndPlayerId(gameId, playerId);
            int existingPowerValue = 0;
            // 새로 선택한 건물 좌표 (중복 카운트 방지)
            Set<String> newBuildingSet = new HashSet<>();
            for (int[] h : buildingHexes) newBuildingSet.add(h[0] + "," + h[1]);
            if (!existingGroups.isEmpty()) {
                GameFederationGroup eg = existingGroups.get(0);
                for (GameFederationBuilding fb : federationBuildingRepository.findByFederationGroupId(eg.getId())) {
                    String key = fb.getHexQ() + "," + fb.getHexR();
                    if (newBuildingSet.contains(key)) continue; // 새 선택과 겹치면 제외 (이중 카운트 방지)
                    GameBuilding b = buildingRepository.findFirstByGameIdAndHexQAndHexRAndIsLantidsMine(gameId, fb.getHexQ(), fb.getHexR(), false).orElse(null);
                    if (b != null) existingPowerValue += buildingPowerValue(b.getBuildingType(), gameId, playerId, b.isHasRing());
                }
            }
            int total = existingPowerValue + totalPowerValue;
            int fedCount = ps.getFederationCount();
            int requiredPower = (fedCount + 1) * 7;
            if (total < requiredPower) {
                return FormFederationResponse.fail(gameId, "연방 파워가 부족합니다 (현재: " + total + ", 다음 목표: " + requiredPower + ")");
            }
        } else {
            if (totalPowerValue < fedRequiredPower) {
                return FormFederationResponse.fail(gameId, "파워 값이 " + fedRequiredPower + " 미만입니다 (현재: " + totalPowerValue + ")");
            }
        }

        // 최소 토큰
        if (!tokenHexes.isEmpty() && canFormWithFewerTokens(selectedBuildings, tokenHexes, buildingHexes, gameId, playerId, isIvits)) {
            return FormFederationResponse.fail(gameId, "최소 토큰 수보다 많은 토큰을 사용했습니다");
        }

        return FormFederationResponse.success(gameId, null, null); // 검증 통과
    }

    public FormFederationResponse formFederation(UUID gameId, FormFederationRequest request) {
        UUID playerId = request.playerId();

        Game game = gameRepository.findById(gameId)
                .orElseThrow(() -> new IllegalArgumentException("게임을 찾을 수 없습니다"));
        if (!"PLAYING".equals(game.getGamePhase())) {
            return FormFederationResponse.fail(gameId, "PLAYING 페이즈가 아닙니다");
        }

        GamePlayerState ps = playerStateRepository.findByGameIdAndPlayerId(gameId, playerId)
                .orElseThrow(() -> new IllegalStateException("플레이어 상태를 찾을 수 없습니다"));

        boolean isIvits = ps.getFactionType() == FactionType.IVITS;

        // 1. 토큰 + 건물 준비
        List<int[]> tokenHexes = request.tokenHexes() != null ? request.tokenHexes() : List.of();

        List<GameBuilding> myBuildings = buildingRepository.findByGameIdAndPlayerId(gameId, playerId);
        Map<String, GameBuilding> myBuildingMap = myBuildings.stream()
                .collect(Collectors.toMap(b -> b.getHexQ() + "," + b.getHexR(), b -> b, (a, c) -> a));

        // 이미 연방된 헥스 + 인접 제외 (하이브는 제외 없음)
        Set<String> usedFedHexes = isIvits ? Set.of() : getUsedFederationHexes(gameId, playerId);
        Set<String> fedBlockedForTokens = new HashSet<>(usedFedHexes);
        if (!isIvits) {
            int[][] adjDirs2 = {{1,0},{-1,0},{0,1},{0,-1},{1,-1},{-1,1}};
            for (String fh : usedFedHexes) {
                String[] p = fh.split(","); int fq = Integer.parseInt(p[0]); int fr = Integer.parseInt(p[1]);
                for (int[] d : adjDirs2) fedBlockedForTokens.add((fq + d[0]) + "," + (fr + d[1]));
            }
        }

        for (int[] hex : tokenHexes) {
            if (fedBlockedForTokens.contains(hex[0] + "," + hex[1])) {
                return FormFederationResponse.fail(gameId, "이미 연방에 사용된 위치에 토큰을 놓았습니다");
            }
        }

        // 토큰이 있으면 토큰 기반 BFS, 없으면 사용자 선택 건물 직접 사용
        List<int[]> buildingHexes;
        if (tokenHexes.isEmpty()) {
            buildingHexes = request.buildingHexes() != null ? request.buildingHexes() : List.of();
            if (buildingHexes.isEmpty()) {
                return FormFederationResponse.fail(gameId, "연방에 포함할 건물을 선택해야 합니다");
            }
            for (int[] hex : buildingHexes) {
                String key = hex[0] + "," + hex[1];
                if (!myBuildingMap.containsKey(key) || usedFedHexes.contains(key)) {
                    return FormFederationResponse.fail(gameId, "유효하지 않은 건물이 포함되어 있습니다: (" + hex[0] + "," + hex[1] + ")");
                }
            }
        } else {
            buildingHexes = findConnectedBuildings(tokenHexes, myBuildingMap, usedFedHexes);
            if (buildingHexes.isEmpty()) {
                return FormFederationResponse.fail(gameId, "토큰에 연결된 건물이 없습니다");
            }
        }

        List<GameBuilding> selectedBuildings = new ArrayList<>();
        for (int[] hex : buildingHexes) {
            selectedBuildings.add(myBuildingMap.get(hex[0] + "," + hex[1]));
        }

        // 2. 하이브: 기존 연방과 합치는 "확장" 로직 / 일반: 이미 사용된 건물 불가
        if (isIvits) {
            return formFederationIvits(gameId, playerId, ps, game, selectedBuildings, buildingHexes, tokenHexes, request.federationTileCode(), request.techTileCode(), request.techTrackCode(), request.coveredTileCode());
        }

        // ── 일반 종족 ──
        // (이미 연방된 건물은 findConnectedBuildings에서 제외됨)

        // 3. 토큰 헥스 검증
        int tokenCount = tokenHexes.size();
        if (tokenCount > 0) {
            int totalPower = ps.getPowerBowl1() + ps.getPowerBowl2() + ps.getPowerBowl3();
            if (totalPower < tokenCount) {
                return FormFederationResponse.fail(gameId, "파워 토큰이 부족합니다 (필요: " + tokenCount + ", 보유: " + totalPower + ")");
            }
        }

        for (int[] hex : tokenHexes) {
            String key = hex[0] + "," + hex[1];
            if (usedFedHexes.contains(key)) {
                return FormFederationResponse.fail(gameId, "이미 연방에 사용된 위치입니다");
            }
            if (myBuildingMap.containsKey(key)) {
                return FormFederationResponse.fail(gameId, "내 건물 위치에는 토큰을 놓을 수 없습니다");
            }
        }

        // 4. 연결성 검증
        Set<String> allHexes = new HashSet<>();
        for (int[] h : buildingHexes) allHexes.add(h[0] + "," + h[1]);
        for (int[] h : tokenHexes) allHexes.add(h[0] + "," + h[1]);
        if (!isConnected(allHexes)) {
            return FormFederationResponse.fail(gameId, "건물과 토큰이 연결되어 있지 않습니다");
        }

        // 5. 파워 값 합계 검증 (제노스 PI: 6 이상, 기본: 7 이상)
        int totalPowerValue = selectedBuildings.stream().mapToInt(b -> b.isLantidsMine() ? 1 : buildingPowerValue(b.getBuildingType(), gameId, playerId, b.isHasRing())).sum();
        // 매안 PI: 본인 행성(TITANIUM) 건물 파워값 +1
        if (ps.getFactionType() == FactionType.BESCODS && ps.getStockPlanetaryInstitute() == 0) {
            for (GameBuilding b : selectedBuildings) {
                var hexOpt = hexRepository.findByGameIdAndHexQAndHexR(gameId, b.getHexQ(), b.getHexR());
                if (hexOpt.isPresent() && hexOpt.get().getPlanetType() == com.gaiaproject.domain.enumtype.player.PlanetType.TITANIUM) {
                    totalPowerValue += 1;
                }
            }
        }
        boolean isXenosPiHere = ps.getFactionType() == FactionType.XENOS && ps.getStockPlanetaryInstitute() == 0;
        int fedReq = isXenosPiHere ? 6 : 7;
        if (totalPowerValue < fedReq) {
            return FormFederationResponse.fail(gameId, "파워 값이 " + fedReq + " 미만입니다 (현재: " + totalPowerValue + ")");
        }

        // 6. 최소 토큰 초과 검증 (최소보다 많이 사용하면 실패)
        if (tokenCount > 0 && canFormWithFewerTokens(selectedBuildings, tokenHexes, buildingHexes, gameId, playerId, isIvits)) {
            return FormFederationResponse.fail(gameId, "최소 토큰 수보다 많은 토큰을 사용했습니다");
        }

        // 7. 연방 타일 검증
        FederationTileType tileType;
        try { tileType = FederationTileType.valueOf(request.federationTileCode()); }
        catch (Exception e) { return FormFederationResponse.fail(gameId, "알 수 없는 연방 타일"); }

        // 8. 처리
        if (tokenCount > 0) { removePowerTokens(ps, tokenCount); }

        GameFederationGroup group = federationGroupRepository.save(GameFederationGroup.builder()
                .gameId(gameId).playerId(playerId).federationTileCode(tileType.name()).build());
        // 비활성 연방토큰(useFederation=false)은 획득 즉시 used 처리
        if (!tileType.isUseFederation()) {
            group.markUsed();
            federationGroupRepository.save(group);
        }
        for (int[] h : buildingHexes) federationBuildingRepository.save(GameFederationBuilding.builder().federationGroupId(group.getId()).hexQ(h[0]).hexR(h[1]).build());
        for (int[] h : tokenHexes) federationTokenHexRepository.save(GameFederationTokenHex.builder().federationGroupId(group.getId()).hexQ(h[0]).hexR(h[1]).build());

        FederationTileService.acquireTileFromOffer(federationOfferRepository, gameId, tileType);
        GamePlayerFederationToken playerToken = playerFederationTokenRepository.save(
                GamePlayerFederationToken.builder().gameId(gameId).playerId(playerId).federationTileType(tileType).build());
        // 비활성 연방토큰(useFederation=false)은 player_token도 즉시 used 처리
        if (!tileType.isUseFederation()) {
            playerToken.markUsed();
            playerFederationTokenRepository.save(playerToken);
        }

        ps.applyIncome(tileType.getImmediateReward());
        if (tileType.getImmediateReward().vp() > 0) {
            vpLogService.logVp(gameId, playerId, com.gaiaproject.domain.enumtype.action.VpCategory.FEDERATION_TOKEN, tileType.getImmediateReward().vp(), null, "연방 토큰: " + tileType.name());
        }
        ps.incrementFederationCount();
        // 라운드 점수: 연방 형성
        if (game.getCurrentRound() != null) {
            roundScoringService.award(gameId, game.getCurrentRound(), ps,
                    com.gaiaproject.domain.enumtype.rounds.RoundScoringEvent.FEDERATION_FORMED, 1);
        }
        playerStateRepository.save(ps);

        log.info("[FEDERATION] 일반 연방 #{}: game={}, player={}, tile={}, buildings={}, tokens={}", ps.getFederationCount(), gameId, playerId, tileType, buildingHexes.size(), tokenCount);

        // 기술 타일 획득 (연방 토큰 → 고급 기술 타일 가능)
        if (request.techTileCode() != null && !request.techTileCode().isEmpty()) {
            techTileService.acquireTileForBuilding(gameId, playerId, request.techTileCode(), request.techTrackCode(), game.getEconomyTrackOption(), request.coveredTileCode());
        }

        // 특수 액션 타일이면 턴을 넘기지 않고 후속 액션 브로드캐스트
        if (tileType.hasSpecialAction()) {
            var specialAction = tileType.getSpecialAction();
            if (specialAction == FederationActionType.TERRAFORM_3_PLACE_MINE) {
                actionService.saveActionOnly(gameId, playerId, ActionType.FACTION_ABILITY, "{\"type\":\"FEDERATION\",\"tileCode\":\"" + tileType.name() + "\"}");
                webSocketService.broadcastDeferredActionRequired(gameId, playerId, "PLACE_MINE_TERRAFORM_3", "{\"terraformDiscount\":3}");
                return FormFederationResponse.success(gameId, tileType.name(), null);
            }
            if (specialAction == FederationActionType.PLACE_MINE_NO_RANGE_LIMIT) {
                actionService.saveActionOnly(gameId, playerId, ActionType.FACTION_ABILITY, "{\"type\":\"FEDERATION\",\"tileCode\":\"" + tileType.name() + "\"}");
                webSocketService.broadcastDeferredActionRequired(gameId, playerId, "PLACE_MINE_NO_RANGE", "{\"noRangeLimit\":true}");
                return FormFederationResponse.success(gameId, tileType.name(), null);
            }
        }

        ConfirmActionResponse result = actionService.saveActionAndNextTurn(gameId, playerId, ActionType.FACTION_ABILITY, "{\"type\":\"FEDERATION\",\"tileCode\":\"" + tileType.name() + "\"}");
        return FormFederationResponse.success(gameId, tileType.name(), result.nextTurnSeatNo());
    }

    /**
     * 하이브 연방: QIC로 연결, 하나의 연방 그룹에 확장, 파워 합이 7의 배수 도달 시 타일 획득
     */
    private FormFederationResponse formFederationIvits(UUID gameId, UUID playerId, GamePlayerState ps, Game game,
                                                        List<GameBuilding> selectedBuildings, List<int[]> buildingHexes,
                                                        List<int[]> tokenHexes, String tileCode,
                                                        String techTileCode, String techTrackCode, String coveredTileCode) {
        int tokenCount = tokenHexes.size();

        // QIC 검증 (파워 토큰 대신 QIC 사용)
        if (tokenCount > 0 && ps.getQic() < tokenCount) {
            return FormFederationResponse.fail(gameId, "QIC가 부족합니다 (필요: " + tokenCount + ", 보유: " + ps.getQic() + ")");
        }

        // 연결성 검증
        Set<String> allHexes = new HashSet<>();
        for (int[] h : buildingHexes) allHexes.add(h[0] + "," + h[1]);
        for (int[] h : tokenHexes) allHexes.add(h[0] + "," + h[1]);

        // 기존 연방 그룹이 있으면 그 그룹의 건물+토큰도 포함하여 연결성 검증
        List<GameFederationGroup> existingGroups = federationGroupRepository.findByGameIdAndPlayerId(gameId, playerId);
        GameFederationGroup existingGroup = existingGroups.isEmpty() ? null : existingGroups.get(0);

        if (existingGroup != null) {
            // 기존 연방 건물/토큰도 연결 대상에 포함
            federationBuildingRepository.findByFederationGroupId(existingGroup.getId())
                    .forEach(b -> allHexes.add(b.getHexQ() + "," + b.getHexR()));
            federationTokenHexRepository.findByFederationGroupId(existingGroup.getId())
                    .forEach(t -> allHexes.add(t.getHexQ() + "," + t.getHexR()));
        }

        if (!isConnected(allHexes)) {
            return FormFederationResponse.fail(gameId, "건물과 QIC 토큰이 연결되어 있지 않습니다");
        }

        // 신규 건물 파워값
        int newPowerValue = selectedBuildings.stream().mapToInt(b -> b.isLantidsMine() ? 1 : buildingPowerValue(b.getBuildingType(), gameId, playerId, b.isHasRing())).sum();

        // 기존 연방 파워 합산
        int existingPowerValue = 0;
        if (existingGroup != null) {
            Set<String> newBuildingSet = new HashSet<>();
            for (int[] h : buildingHexes) newBuildingSet.add(h[0] + "," + h[1]);
            List<GameFederationBuilding> existingFedBuildings = federationBuildingRepository.findByFederationGroupId(existingGroup.getId());
            for (GameFederationBuilding fb : existingFedBuildings) {
                String key = fb.getHexQ() + "," + fb.getHexR();
                if (!newBuildingSet.contains(key)) {
                    GameBuilding b = buildingRepository.findFirstByGameIdAndHexQAndHexRAndIsLantidsMine(gameId, fb.getHexQ(), fb.getHexR(), false).orElse(null);
                    if (b != null) existingPowerValue += buildingPowerValue(b.getBuildingType(), gameId, playerId, b.isHasRing());
                }
            }
        }

        int totalPowerValue = existingPowerValue + newPowerValue;

        // 카운터 기반: 다음 연방 목표 = (형성횟수+1)*7
        int fedCount = ps.getFederationCount();
        int requiredPower = (fedCount + 1) * 7;
        if (totalPowerValue < requiredPower) {
            return FormFederationResponse.fail(gameId,
                    "연방 파워가 부족합니다 (현재: " + totalPowerValue + ", 다음 목표: " + requiredPower + ")");
        }

        // 연방 타일 검증
        FederationTileType tileType;
        try { tileType = FederationTileType.valueOf(tileCode); }
        catch (Exception e) { return FormFederationResponse.fail(gameId, "알 수 없는 연방 타일"); }

        // 최소 QIC 초과 검증
        if (tokenCount > 0 && canFormWithFewerTokens(selectedBuildings, tokenHexes, buildingHexes, gameId, playerId, true)) {
            return FormFederationResponse.fail(gameId, "최소 QIC 수보다 많은 QIC를 사용했습니다");
        }

        // QIC 차감
        if (tokenCount > 0) { ps.spendQic(tokenCount); }

        // 그룹 생성 또는 기존 그룹에 확장
        GameFederationGroup group;
        if (existingGroup != null) {
            group = existingGroup;
        } else {
            group = federationGroupRepository.save(GameFederationGroup.builder()
                    .gameId(gameId).playerId(playerId).federationTileCode(tileType.name()).build());
            // 비활성 연방토큰(useFederation=false)은 획득 즉시 used 처리
            if (!tileType.isUseFederation()) {
                group.markUsed();
                federationGroupRepository.save(group);
            }
        }

        // 건물/토큰 추가 (기존 연방에 이미 있는 건물은 중복 저장하지 않음)
        Set<String> existingBuildingKeys = new HashSet<>();
        if (existingGroup != null) {
            federationBuildingRepository.findByFederationGroupId(group.getId())
                    .forEach(fb -> existingBuildingKeys.add(fb.getHexQ() + "," + fb.getHexR()));
        }
        for (int[] h : buildingHexes) {
            if (!existingBuildingKeys.contains(h[0] + "," + h[1])) {
                federationBuildingRepository.save(GameFederationBuilding.builder().federationGroupId(group.getId()).hexQ(h[0]).hexR(h[1]).build());
            }
        }
        Set<String> existingTokenKeys = new HashSet<>();
        if (existingGroup != null) {
            federationTokenHexRepository.findByFederationGroupId(group.getId())
                    .forEach(th -> existingTokenKeys.add(th.getHexQ() + "," + th.getHexR()));
        }
        for (int[] h : tokenHexes) {
            if (!existingTokenKeys.contains(h[0] + "," + h[1])) {
                federationTokenHexRepository.save(GameFederationTokenHex.builder().federationGroupId(group.getId()).hexQ(h[0]).hexR(h[1]).build());
            }
        }

        // 연방 타일 획득
        FederationTileService.acquireTileFromOffer(federationOfferRepository, gameId, tileType);
        GamePlayerFederationToken playerToken = playerFederationTokenRepository.save(
                GamePlayerFederationToken.builder().gameId(gameId).playerId(playerId).federationTileType(tileType).build());
        // 비활성 연방토큰(useFederation=false)은 player_token도 즉시 used 처리
        if (!tileType.isUseFederation()) {
            playerToken.markUsed();
            playerFederationTokenRepository.save(playerToken);
        }

        ps.applyIncome(tileType.getImmediateReward());
        if (tileType.getImmediateReward().vp() > 0) {
            vpLogService.logVp(gameId, playerId, com.gaiaproject.domain.enumtype.action.VpCategory.FEDERATION_TOKEN, tileType.getImmediateReward().vp(), null, "연방 토큰: " + tileType.name());
        }
        ps.incrementFederationCount();
        // 라운드 점수: 연방 형성
        if (game.getCurrentRound() != null) {
            roundScoringService.award(gameId, game.getCurrentRound(), ps,
                    com.gaiaproject.domain.enumtype.rounds.RoundScoringEvent.FEDERATION_FORMED, 1);
        }
        playerStateRepository.save(ps);

        log.info("[FEDERATION-IVITS] 연방 #{}: game={}, player={}, tile={}, newBuildings={}, qicTokens={}, totalPower={}",
                ps.getFederationCount(), gameId, playerId, tileType, buildingHexes.size(), tokenCount, totalPowerValue);

        // 기술 타일 획득
        if (techTileCode != null && !techTileCode.isEmpty()) {
            techTileService.acquireTileForBuilding(gameId, playerId, techTileCode, techTrackCode, game.getEconomyTrackOption(), coveredTileCode);
        }

        // 특수 액션 타일이면 턴을 넘기지 않고 후속 액션 브로드캐스트
        if (tileType.hasSpecialAction()) {
            var specialAction2 = tileType.getSpecialAction();
            if (specialAction2 == FederationActionType.TERRAFORM_3_PLACE_MINE) {
                actionService.saveActionOnly(gameId, playerId, ActionType.FACTION_ABILITY, "{\"type\":\"FEDERATION_IVITS\",\"tileCode\":\"" + tileType.name() + "\"}");
                webSocketService.broadcastDeferredActionRequired(gameId, playerId, "PLACE_MINE_TERRAFORM_3", "{\"terraformDiscount\":3}");
                return FormFederationResponse.success(gameId, tileType.name(), null);
            }
            if (specialAction2 == FederationActionType.PLACE_MINE_NO_RANGE_LIMIT) {
                actionService.saveActionOnly(gameId, playerId, ActionType.FACTION_ABILITY, "{\"type\":\"FEDERATION_IVITS\",\"tileCode\":\"" + tileType.name() + "\"}");
                webSocketService.broadcastDeferredActionRequired(gameId, playerId, "PLACE_MINE_NO_RANGE", "{\"noRangeLimit\":true}");
                return FormFederationResponse.success(gameId, tileType.name(), null);
            }
        }

        ConfirmActionResponse result = actionService.saveActionAndNextTurn(gameId, playerId, ActionType.FACTION_ABILITY, "{\"type\":\"FEDERATION_IVITS\",\"tileCode\":\"" + tileType.name() + "\"}");
        return FormFederationResponse.success(gameId, tileType.name(), result.nextTurnSeatNo());
    }

    /** 플레이어의 이미 사용된 연방 헥스 (건물 + 토큰) 조회 */
    /** 기존 연방 토큰 헥스만 반환 (건물 제외) */
    private Set<String> getUsedFederationTokenHexes(UUID gameId, UUID playerId) {
        List<GameFederationGroup> groups = federationGroupRepository.findByGameIdAndPlayerId(gameId, playerId);
        if (groups.isEmpty()) return Set.of();
        List<UUID> groupIds = groups.stream().map(GameFederationGroup::getId).toList();
        Set<String> used = new HashSet<>();
        federationTokenHexRepository.findByFederationGroupIdIn(groupIds)
                .forEach(t -> used.add(t.getHexQ() + "," + t.getHexR()));
        return used;
    }

    private Set<String> getUsedFederationHexes(UUID gameId, UUID playerId) {
        List<GameFederationGroup> groups = federationGroupRepository.findByGameIdAndPlayerId(gameId, playerId);
        if (groups.isEmpty()) return Set.of();

        List<UUID> groupIds = groups.stream().map(GameFederationGroup::getId).toList();
        Set<String> used = new HashSet<>();

        federationBuildingRepository.findByFederationGroupIdIn(groupIds)
                .forEach(b -> used.add(b.getHexQ() + "," + b.getHexR()));
        federationTokenHexRepository.findByFederationGroupIdIn(groupIds)
                .forEach(t -> used.add(t.getHexQ() + "," + t.getHexR()));

        return used;
    }

    /** 연결성 검증 (BFS) */
    private boolean isConnected(Set<String> hexes) {
        if (hexes.size() <= 1) return true;
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();
        String start = hexes.iterator().next();
        queue.add(start);
        visited.add(start);

        while (!queue.isEmpty()) {
            String current = queue.poll();
            String[] parts = current.split(",");
            int q = Integer.parseInt(parts[0]);
            int r = Integer.parseInt(parts[1]);

            // 6방향 이웃
            int[][] neighbors = {{1,0},{-1,0},{0,1},{0,-1},{1,-1},{-1,1}};
            for (int[] n : neighbors) {
                String neighbor = (q + n[0]) + "," + (r + n[1]);
                if (hexes.contains(neighbor) && !visited.contains(neighbor)) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }
        return visited.size() == hexes.size();
    }

    /** 사용된 토큰 수가 최소보다 많은지 검증 */
    private boolean canFormWithFewerTokens(List<GameBuilding> buildings, List<int[]> tokenHexes, List<int[]> buildingHexes,
                                            UUID gameId, UUID playerId, boolean isIvits) {
        if (tokenHexes == null || tokenHexes.isEmpty()) return false;
        Set<String> buildingSet = new HashSet<>();
        for (int[] h : buildingHexes) buildingSet.add(h[0] + "," + h[1]);
        // 하이브: 기존 연방 건물/토큰도 0-비용 연결 노드로 포함 (이미 QIC 지불된 곳)
        List<int[]> effectiveBuildingHexes = buildingHexes;
        if (isIvits) {
            List<GameFederationGroup> existingGroups = federationGroupRepository.findByGameIdAndPlayerId(gameId, playerId);
            if (!existingGroups.isEmpty()) {
                UUID gid = existingGroups.get(0).getId();
                for (GameFederationBuilding fb : federationBuildingRepository.findByFederationGroupId(gid)) {
                    buildingSet.add(fb.getHexQ() + "," + fb.getHexR());
                }
                for (var ft : federationTokenHexRepository.findByFederationGroupId(gid)) {
                    buildingSet.add(ft.getHexQ() + "," + ft.getHexR());
                }
                effectiveBuildingHexes = buildingSet.stream()
                        .map(s -> { String[] p = s.split(","); return new int[]{Integer.parseInt(p[0]), Integer.parseInt(p[1])}; })
                        .toList();
            }
        }
        Set<String> allowedHexes = buildAllowedHexes(gameId, playerId, isIvits);
        int minTokens = calcMinTokensToConnect(effectiveBuildingHexes, gameId, allowedHexes, buildingSet);
        return tokenHexes.size() > minTokens;
    }

    /** 파워 토큰 제거 (bowl1 → bowl2 → bowl3 순) */
    private void removePowerTokens(GamePlayerState ps, int count) {
        int remaining = count;
        int fromBowl1 = Math.min(ps.getPowerBowl1(), remaining);
        if (fromBowl1 > 0) { ps.removePowerFromBowl1(fromBowl1); remaining -= fromBowl1; }
        int fromBowl2 = Math.min(ps.getPowerBowl2(), remaining);
        if (fromBowl2 > 0) { ps.removePowerFromBowl2(fromBowl2); remaining -= fromBowl2; }
        if (remaining > 0) { ps.removePowerFromBowl3(remaining); }
    }

    private int buildingPowerValue(BuildingType type) {
        return switch (type) {
            case MINE, LOST_PLANET_MINE -> 1;
            case TRADING_STATION, RESEARCH_LAB -> 2;
            case PLANETARY_INSTITUTE, ACADEMY -> 3;
            case SPACE_STATION -> 1;
            default -> 0;
        };
    }

    /** BASIC_TILE_9 보유 시 큰 건물 파워 가치 +1, 모웨이드 링 +2 반영 */
    private int buildingPowerValue(BuildingType type, UUID gameId, UUID playerId) {
        return buildingPowerValue(type, gameId, playerId, false);
    }

    private int buildingPowerValue(BuildingType type, UUID gameId, UUID playerId, boolean hasRing) {
        int base = buildingPowerValue(type);
        if ((type == BuildingType.PLANETARY_INSTITUTE || type == BuildingType.ACADEMY)
                && playerTechTileRepository
                    .findByGameIdAndPlayerIdAndIsCovered(gameId, playerId, false)
                    .stream()
                    .anyMatch(t -> "BASIC_TILE_9".equals(t.getTechTileCode()))) {
            base += 1;
        }
        if (hasRing) base += 2;
        return base;
    }

    /** 게임의 모든 연방 데이터 조회 (FE 표시용) — 연방 그룹 + 플레이어 직접 보유 토큰(글린 PI 등) */
    public List<FederationGroupInfo> getFederationGroups(UUID gameId) {
        List<FederationGroupInfo> result = new java.util.ArrayList<>();

        // 1. 연방 그룹 기반 토큰
        List<GameFederationGroup> groups = federationGroupRepository.findByGameId(gameId);
        for (var g : groups) {
            // 좌표 기준 중복 제거 (하이브: 같은 건물이 여러 번 추가될 수 있음)
            List<int[]> bHexes = federationBuildingRepository.findByFederationGroupId(g.getId())
                    .stream().map(b -> new int[]{b.getHexQ(), b.getHexR()})
                    .collect(java.util.stream.Collectors.toMap(
                            h -> h[0] + "," + h[1], h -> h, (a, b) -> a))
                    .values().stream().toList();
            List<int[]> tHexes = federationTokenHexRepository.findByFederationGroupId(g.getId())
                    .stream().map(t -> new int[]{t.getHexQ(), t.getHexR()})
                    .collect(java.util.stream.Collectors.toMap(
                            h -> h[0] + "," + h[1], h -> h, (a, b) -> a))
                    .values().stream().toList();
            result.add(new FederationGroupInfo(g.getPlayerId().toString(), g.getFederationTileCode(), bHexes, tHexes, g.isUsed()));
        }

        // 2. 플레이어 직접 보유 토큰 (글린 PI, 하이브 중복 토큰 등)
        // 연방 그룹에 이미 포함된 개수보다 보유 토큰이 더 많으면 추가
        var allPlayerTokens = playerFederationTokenRepository.findByGameId(gameId);
        for (var token : allPlayerTokens) {
            String tileCode = token.getFederationTileType().name();
            String pid = token.getPlayerId().toString();
            long countInResult = result.stream().filter(
                    r -> r.playerId().equals(pid) && r.tileCode().equals(tileCode)
            ).count();
            long countInTokens = allPlayerTokens.stream().filter(
                    t -> t.getPlayerId().toString().equals(pid) && t.getFederationTileType().name().equals(tileCode)
            ).count();
            if (countInResult < countInTokens) {
                result.add(new FederationGroupInfo(pid, tileCode, List.of(), List.of(), token.isUsed()));
            }
        }

        return result;
    }

    /**
     * 건물 건설/업그레이드 시 인접 연방에 자동 편입.
     * 새 건물의 6방향 이웃에 해당 플레이어의 연방 소속 건물이나 토큰이 있으면 같은 그룹에 추가.
     */
    public void autoJoinFederation(UUID gameId, UUID playerId, int hexQ, int hexR) {
        List<GameFederationGroup> groups = federationGroupRepository.findByGameIdAndPlayerId(gameId, playerId);
        if (groups.isEmpty()) return;

        int[][] neighbors = {{1,0},{-1,0},{0,1},{0,-1},{1,-1},{-1,1}};

        for (GameFederationGroup group : groups) {
            List<GameFederationBuilding> fedBuildings = federationBuildingRepository.findByFederationGroupId(group.getId());
            List<GameFederationTokenHex> fedTokens = federationTokenHexRepository.findByFederationGroupId(group.getId());

            Set<String> fedHexes = new HashSet<>();
            fedBuildings.forEach(b -> fedHexes.add(b.getHexQ() + "," + b.getHexR()));
            fedTokens.forEach(t -> fedHexes.add(t.getHexQ() + "," + t.getHexR()));

            // 이미 이 그룹에 포함되어 있으면 스킵
            if (fedHexes.contains(hexQ + "," + hexR)) return;

            // 새 건물의 이웃 중 이 연방에 속하는 헥스가 있는지 확인
            boolean adjacent = false;
            for (int[] n : neighbors) {
                if (fedHexes.contains((hexQ + n[0]) + "," + (hexR + n[1]))) {
                    adjacent = true;
                    break;
                }
            }

            if (adjacent) {
                federationBuildingRepository.save(GameFederationBuilding.builder()
                        .federationGroupId(group.getId()).hexQ(hexQ).hexR(hexR).build());
                log.info("[FEDERATION] 자동 편입: game={}, player={}, hex=({},{}), group={}", gameId, playerId, hexQ, hexR, group.getId());
                return; // 하나의 그룹에만 편입
            }
        }
    }

    /**
     * 토큰 헥스에 인접한 내 건물을 BFS로 탐색하여 연결된 건물 전부 반환.
     * 토큰끼리도 연결되고, 토큰-건물, 건물-건물도 인접하면 연결.
     */
    private List<int[]> findConnectedBuildings(List<int[]> tokenHexes, Map<String, GameBuilding> myBuildingMap, Set<String> excludeHexes) {
        // 토큰 + 내 모든 건물을 합쳐서 연결 그래프 구성
        Set<String> tokenSet = new HashSet<>();
        for (int[] t : tokenHexes) tokenSet.add(t[0] + "," + t[1]);

        // BFS: 토큰에서 시작, 인접한 내 건물 탐색, 건물에서도 인접 건물/토큰 탐색
        Set<String> visited = new HashSet<>();
        Queue<String> queue = new LinkedList<>();

        // 시드: 모든 토큰 + 토큰에 인접한 내 건물
        for (int[] t : tokenHexes) {
            String key = t[0] + "," + t[1];
            if (!visited.contains(key)) {
                visited.add(key);
                queue.add(key);
            }
        }

        int[][] dirs = {{1,0},{-1,0},{0,1},{0,-1},{1,-1},{-1,1}};
        Set<String> connectedBuildings = new LinkedHashSet<>();

        while (!queue.isEmpty()) {
            String current = queue.poll();
            String[] parts = current.split(",");
            int q = Integer.parseInt(parts[0]);
            int r = Integer.parseInt(parts[1]);

            // 현재 위치가 내 건물이고 아직 연방에 속하지 않으면 포함
            if (myBuildingMap.containsKey(current) && !excludeHexes.contains(current)) {
                connectedBuildings.add(current);
            }

            for (int[] d : dirs) {
                String neighbor = (q + d[0]) + "," + (r + d[1]);
                if (visited.contains(neighbor)) continue;
                // 이웃이 토큰이거나 (이미 연방되지 않은) 내 건물이면 탐색 대상
                if (tokenSet.contains(neighbor) || (myBuildingMap.containsKey(neighbor) && !excludeHexes.contains(neighbor))) {
                    visited.add(neighbor);
                    queue.add(neighbor);
                }
            }
        }

        return connectedBuildings.stream()
                .map(k -> { String[] p = k.split(","); return new int[]{Integer.parseInt(p[0]), Integer.parseInt(p[1])}; })
                .toList();
    }

    public record FederationGroupInfo(String playerId, String tileCode, List<int[]> buildingHexes, List<int[]> tokenHexes, boolean used) {}

    // ================================================================
    // C안 commit-turn 지원: raw DB operation (검증 없음)
    // ================================================================

    /**
     * 연방 그룹 raw 생성 — 검증 없음, FE가 계산한 결과 그대로 저장.
     * - game_federation_group INSERT
     * - game_federation_building rows INSERT
     * - game_federation_token_hex rows INSERT
     * - game_player_federation_token INSERT
     * - FederationTileType.isUseFederation() == false 이면 즉시 used 처리
     */
    public void createGroupRaw(UUID gameId, UUID playerId, String tileCode,
                               List<int[]> buildingHexes, List<int[]> tokenHexes) {
        FederationTileType tileType;
        try {
            tileType = FederationTileType.valueOf(tileCode);
        } catch (Exception e) {
            log.warn("[COMMIT_TURN] 알 수 없는 연방 타일 코드: {}", tileCode);
            return;
        }

        GameFederationGroup group = federationGroupRepository.save(GameFederationGroup.builder()
                .gameId(gameId).playerId(playerId).federationTileCode(tileType.name()).build());
        if (!tileType.isUseFederation()) {
            group.markUsed();
            federationGroupRepository.save(group);
        }
        if (buildingHexes != null) {
            for (int[] h : buildingHexes) {
                federationBuildingRepository.save(GameFederationBuilding.builder()
                        .federationGroupId(group.getId()).hexQ(h[0]).hexR(h[1]).build());
            }
        }
        if (tokenHexes != null) {
            for (int[] h : tokenHexes) {
                federationTokenHexRepository.save(GameFederationTokenHex.builder()
                        .federationGroupId(group.getId()).hexQ(h[0]).hexR(h[1]).build());
            }
        }

        // 오퍼에서 타일 차감
        FederationTileService.acquireTileFromOffer(federationOfferRepository, gameId, tileType);

        // 플레이어 토큰 기록 (비활성 연방토큰은 즉시 used 처리)
        GamePlayerFederationToken pToken = playerFederationTokenRepository.save(GamePlayerFederationToken.builder()
                .gameId(gameId).playerId(playerId).federationTileType(tileType).build());
        if (!tileType.isUseFederation()) {
            pToken.markUsed();
            playerFederationTokenRepository.save(pToken);
        }

        log.info("[COMMIT_TURN] 연방 그룹 생성: game={}, player={}, tile={}, buildings={}, tokens={}",
                gameId, playerId, tileType,
                buildingHexes != null ? buildingHexes.size() : 0,
                tokenHexes != null ? tokenHexes.size() : 0);
    }

    /**
     * 연방 토큰 플립 (used 처리) — raw.
     * game_federation_group 과 game_player_federation_token 양쪽 동기화.
     */
    public void flipTokenRaw(UUID gameId, UUID playerId, String tileCode) {
        // 그룹 플립 (사용 안 된 것 중 하나)
        List<GameFederationGroup> groups = federationGroupRepository.findByGameIdAndPlayerId(gameId, playerId);
        for (GameFederationGroup g : groups) {
            if (tileCode.equals(g.getFederationTileCode()) && !g.isUsed()) {
                g.markUsed();
                federationGroupRepository.save(g);
                break;
            }
        }
        // 플레이어 토큰 플립
        var tokens = playerFederationTokenRepository.findByGameIdAndPlayerId(gameId, playerId);
        for (var t : tokens) {
            if (tileCode.equals(t.getFederationTileType().name()) && !t.isUsed()) {
                t.markUsed();
                playerFederationTokenRepository.save(t);
                break;
            }
        }
        log.info("[COMMIT_TURN] 연방 토큰 플립: game={}, player={}, tile={}", gameId, playerId, tileCode);
    }
}
