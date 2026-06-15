# Gaia Project 백엔드 심층 분석 보고서

보드게임 "Gaia Project" 온라인 구현체 중 **서버 측** (Spring Boot 4 / Java 21 / PostgreSQL 16) 을 깊이 분석한 문서다. 도메인 모델, 서비스 계층, WebSocket 통신, 게임 페이즈 전환, 그리고 핵심 비즈니스 규칙이 실제 코드에서 어떻게 구현되어 있는지 정리한다.

---

## 1. 기술 스택 & 빌드 설정

### 1.1 핵심 기술
- **Java 21** (LTS), `build.gradle`의 `JavaLanguageVersion.of(21)`
- **Spring Boot 4.0.1** (spring-boot-starter-web, data-jpa, websocket, validation)
- **Hibernate / JPA** + **Flyway** (DDL은 모두 마이그레이션으로 관리, `ddl-auto: none`)
- **PostgreSQL 16** (`jdbc:postgresql://localhost:5432/gaiadb`)
- **STOMP WebSocket** (spring-boot-starter-websocket)
- **OpenAPI / Swagger 3.x** (`springdoc-openapi-starter-webmvc-ui`)
- **Lombok** (엔티티 보일러플레이트 제거)

### 1.2 실행 환경
- 포트: **9000**
- DB 커넥션 풀: HikariCP (max 15, min-idle 5, timeout 30s)
- Hibernate 배치 설정: `batch_size=20`, `order_inserts=true`
- `docker-compose.yml` — PostgreSQL 16, `gaiadb/gaia/gaia`, 포트 5432
- `Dockerfile` — BE 이미지 빌드

### 1.3 주요 파일 진입점
```
gaia-project/
├── build.gradle, settings.gradle
├── docker-compose.yml, Dockerfile
├── src/main/
│   ├── java/com/gaiaproject/   ← 애플리케이션 소스 (약 197 파일)
│   └── resources/
│       ├── application.yml
│       └── db/migration/       ← Flyway V1, V2
```

---

## 2. 패키지 구조 (한눈에 보기)

```
com.gaiaproject/
├── GaiaProjectApplication.java     엔트리포인트
├── config/                         SecurityConfig · WebSocketConfig · WebSocketEventListener
├── controller/                     13개 REST 컨트롤러
├── service/                        28개 서비스 (게임 룰의 중심)
├── domain/
│   ├── entity/                     45+ JPA 엔티티
│   │   ├── game/                   Game, GameSeat, GameParticipant, GameAction, GameBid, ...
│   │   ├── player/                 GamePlayerState, Player, RegisteredPlayer, GamePlayer* 타일
│   │   ├── building/               GameBuilding
│   │   ├── map/                    GameHex, GameSectorPlacement, GameSingleHexPlacement
│   │   ├── federation/             GameFederationGroup / Building / TokenHex / Offer
│   │   ├── artifact/               GameArtifactOffer · GamePlayerArtifact
│   │   ├── booster/                GameBoosterOffer · GamePlayerRoundBooster
│   │   ├── tech/                   GameTechOffer · GameAdvTechOffer · GamePlayerTechTile
│   │   ├── leech/                  GameLeechOffer
│   │   └── rounds/                 GameRoundScoring · GameFinalScoring
│   └── enumtype/                   50+ 열거형 (FactionType, ActionType, PowerActionType, ...)
├── dto/                            Request / Response / VO / WebSocket 이벤트
├── repository/                     40+ JPA Repository
└── util/                           HexUtil (axial 좌표 거리 계산 등)
```

---

## 3. 핵심 엔티티

### 3.1 `Game` — 게임 세션
필드 요약:
- `id (UUID)`, `roomCode (8자리)`, `status (READY / IN_PROGRESS / FINISHED)`
- `gamePhase` — `MAP_ROTATE → SETUP_MINE_FIRST/SECOND/XENOS/EXPANSION → BOOSTER_SELECTION → BIDDING / BID_SEAT_PICK → PLAYING`
- `currentRound (1~6)`, `currentTurnSeatNo (1~4)`
- `economyTrackOption` (`OPTION_A / OPTION_B`, 방 생성 시 랜덤)
- `commonAdvTileCondition` (`VP_25 / FLEET_3`)
- `setupMineIndex`, `setupMineOrder (JSON)` — snake draft 순서 `[1,2,3,4,4,3,2,1,...]`
- 비딩 필드 — `biddingRound`, `biddingCurrentBid`, `biddingTurnPlayerId`
- 확장 종족 — `tinkeroidsExtraRingPlanet`, `moweidsExtraRingPlanet`
- `@Version long version` — 낙관적 락

대표 메서드: `startGame()`, `startInitialMinePlacement(order)`, `nextMinePlacement()`, `startBidding()`, `nextBiddingRound(firstPlayerId)`, `startPlaying()`.

### 3.2 `GamePlayerState` — 플레이어 자원·기술·건물 재고
- **자원**: `ore (0~15)`, `credit (0~30)`, `knowledge (0~15)`, `qic (무제한)`
- **파워 Bowl**: `powerBowl1 / 2 / 3` — Gaia Project 고유 순환 메커니즘
- **가이아 트랙**: `techGaia (0~5)`, `gaiaPower`, `stockGaiaformer (0~3)`, `permanentlyRemovedGaiaformers`
- **기술 트랙**: `techTerraforming / Navigation / Ai / Economy / Science` (각 0~5)
- **건물 재고**: `stockMine (8)`, `stockTradingStation (4)`, `stockResearchLab (3)`, `stockPlanetaryInstitute (1)`, `stockAcademy (2)`
- **팩션 특수 상태**:
  - `brainstoneBowl` (타클론)
  - `baltaksConvertedGaiaformers` (발타크)
  - `tinkeroidsUsedActions`, `tinkeroidsCurrentAction` (팅커로이드)
  - `gleensHasQicAcademy` (글린)
- **라운드 1회성 플래그**: `boosterActionUsed`, `factionAbilityUsed`, `qicAcademyActionUsed`
- **통계**: `victoryPoints`, `bidPenalty`, `federationCount`, `usedTimeSeconds`

핵심 메서드
- `addOre/Credit/Knowledge/Qic(amount)` — 상한 반영 가산
- `chargePower(amount)` / `chargePowerWithFactionRules(amount)` — bowl1→2→3 순환, 타클론 브레인스톤 우선 규칙
- `spendPower(amount)` — bowl3 사용, bowl1로 복귀
- `spendPowerToGaia(required)` — 가이아 포밍 비용 지불 (bowl1·2·3 모두 투하)
- `burnPower()` — `bowl2 -2 / bowl3 +1`, 아이타는 `gaiaPower +1` 추가
- `advanceTechTrack(code)` — 지식 4 차감 후 트랙 전진
- `convertGaiaformerToQic()` — 발타크 특수
- `selectTinkeroidsAction(code)`
- `applySnapshot(PlayerStateSnapshot)` — **C안 commit-turn 의 핵심**: FE가 계산한 최종 상태로 통째로 덮어씀

### 3.3 `GameBuilding`
- `(gameId, playerId, hexQ, hexR)` + `buildingType` (`MINE`, `TRADING_STATION`, `RESEARCH_LAB`, `PLANETARY_INSTITUTE`, `ACADEMY`, `GAIAFORMER`, `SPACE_STATION`, `LOST_PLANET_MINE`)
- `academyType (KNOWLEDGE / QIC)` — 아카데미 전용
- `isLantidsMine` — 란티다 공유 광산
- `hasRing` — 모웨이드 Ring (파워값 +2)
- 팩토리: `place()`, `upgrade(newType)`, `upgradeToAcademy(type)`

### 3.4 `GameSeat`
- `seatNo (1~4)`, `turnOrder`, `factionType`, `playerId`, `joinedAt`
- `claim(playerId)` — 좌석 선점

### 3.5 `GameHex`
- 복합키 `(gameId, hexQ, hexR)`, `planetType`, `sectorId`, `positionNo`
- `PlanetType`: `TERRA`, `DESERT`, `SWAMP`, `VOLCANIC`, `OXIDE`, `TITANIUM`, `ICE`, `GAIA`, `TRANSDIM`, `LOST_PLANET`

---

## 4. Enum 도메인 언어

### 4.1 `FactionType` — 18종
- **기본 8종**: XENOS(DESERT), GLEENS(DESERT), TAKLONS(SWAMP), AMBAS(SWAMP), HADSCH_HALLAS(VOLCANIC), IVITS(VOLCANIC), GEODENS(OXIDE), BAL_TAKS(OXIDE)
- **지구형 2종**: LANTIDS(TERRA), TERRANS(TERRA)
- **티타늄/아이스 4종**: FIRAKS(TITANIUM), BESCODS(TITANIUM), NEVLAS(ICE), ITARS(ICE)
- **확장 4종**: SPACE_GIANTS(LOST_PLANET), MOWEIDS(LOST_PLANET), TINKEROIDS(ASTEROIDS), DAKANIANS(ASTEROIDS)

각 팩션은 아래 메서드를 갖는다.
- `getBaseIncome()` — 라운드별 기본 수입
- `getPiIncome()` — PI 건설 후 수입 보너스
- `getInitialResources()` — 초기 자원
- `getInitialTechTracks()` — 초기 트랙 레벨
- `getInitialGaiaformers()`
- 정적 `getRandomFourDifferentHomePlanets()` — 방 생성시 행성 타입이 서로 다른 4종 랜덤 선택

### 4.2 `ActionType`
`PLACE_MINE · UPGRADE_BUILDING · POWER_ACTION · FLEET_ACTION · ADVANCE_TECH · PASS · DEPLOY_GAIAFORMER · FLEET_SHIP_ACTION · FACTION_ABILITY · POWER_INCOME · BOOSTER_ACTION · FORM_FEDERATION · TECH_TILE_ACTION · QIC_ACADEMY_ACTION`

### 4.3 `PowerActionType` — 31종
기본 파워 액션 (PWR_KNOWLEDGE / PWR_TERRAFORM_2 / PWR_ORE / PWR_CREDIT / PWR_TOKEN …) + 함대 파워 액션 (TF_MARS, ECLIPSE, REBELLION, TWILIGHT 각 3개).

### 4.4 `BuildingType` & 기타
- BuildingType 파워값: MINE=1, TRADING_STATION=2, RESEARCH_LAB=3, PI=4, ACADEMY=5, GAIAFORMER=6(연방 불가), SPACE_STATION=1(하이브 전용), LOST_PLANET_MINE=1
- TechTileCode / AdvancedTechTileCode / TechTrackOption / EconomyTrackOption
- RoundBoosterType (8종) · BoosterActionType
- FederationTileType (`FED_6`, `FED_7`, `FED_8`, `FED_9`, `FED_10`, `FED_QIC` …)
- ArtifactType / ArtifactEffectType
- RoundScoringTileType / FinalScoringTileType / RoundScoringEvent

---

## 5. 서비스 레이어 — 28개 서비스의 역할

| 서비스 | 책임 |
|---|---|
| **GameService** | 방 생성/입장, 좌석 세팅, 페이즈 전환, 맵/타일/부스터/연방/아티팩트 초기화 |
| **ActionService** | 턴 타이머, 액션 로깅, 다음 턴 이동, 브로드캐스트 |
| **BuildingService** | 초기 광산 / 인플레이 광산 / 업그레이드, 파워 리치 트리거 |
| **TechTileService** | 기본·고급 타일 offer, 기술 트랙 전진, 타일 액션 |
| **PowerActionService** | 파워 액션 사용, 파워 소각 |
| **PowerLeechService** | 리치 제안 생성, 자동/수동 결정 처리, 타클론 특례 |
| **IncomeService** | 라운드 수입 계산 & 적용 (6단계 수입원) |
| **PassService** | 패스 VP 계산, 부스터 교체, 라운드 종료 판정 |
| **RoundBoosterService** | 부스터 offer 관리, 부스터 액션 실행 |
| **FederationFormService** | 연방 유효성 검증, 토큰 배치, 보상 지급 |
| **FederationTileService** | 연방 타일 offer 관리 |
| **FleetService** | 함대 탐사선 (QIC 소모) 배치 |
| **FleetShipActionService** | TF_MARS / ECLIPSE / REBELLION / TWILIGHT 특수 액션 |
| **GaiaformingService** | 가이아 포머 배치, 테라포밍 비용 지불 |
| **ArtifactService** | 아티팩트 획득/효과 |
| **BiddingService** | 초기 좌석 비딩 (BIDDING → BID_SEAT_PICK) |
| **FactionAbilityService** | 팩션 고유 능력 사용 분기 |
| **MapService** | 섹터 랜덤 배치, 헥스 초기화 |
| **GameWebSocketService** | `/topic/room/{roomId}` 브로드캐스트 |
| **GameSnapshotService** | 전체 상태 스냅샷 재구성 |
| **CommitTurnService** | **C안**: FE 스냅샷으로 BE 상태 덮어쓰기 + 다음 턴 이동 |
| **GameCalculationService** | 거리, 라운드 / 최종 점수, 조건 판정 |
| **GameEndScoringService** | 최종 점수 + 비딩 패널티 |
| **RoundScoringService** | 라운드 미션 VP 계산 |
| **VpLogService** | VP 변동 이력 (카테고리 · 사유) |
| **FreeConvertService** | 자유 액션 자원 변환 |
| **MineSetupOrderService** | 초기 광산 snake-draft 순서 결정 |
| **PassBoosterService 등** | 세부 파사드 |

### 5.1 GameService 핵심 흐름
- `createRoom(CreateRoomRequest)` — Game 생성(READY), 4 좌석 + 랜덤 팩션, 부스터/타일/연방/아티팩트/맵 초기화, `roomCode` 반환
- `enterRoom(roomId, EnterGameRequest)` — 닉네임 검증, `rejoinToken` 발급, 4명 완료 시 `gamePhase=MAP_ROTATE`
- `startBidding(roomId)` — 비딩 수동 시작
- `startGame(roomId)` — 부스터 선택 완료 후 `PLAYING` 진입
- `startNewRound(roomId)` — 라운드 2~6 시작 (IncomeService 호출 포함)
- `getPublicState / getPlayerStates`

### 5.2 IncomeService — 라운드 수입 6단계
라운드 시작 시 `applyRoundIncome(gameId)` 호출. 플래그 리셋(boosterActionUsed, factionAbilityUsed 등) + 테란 PI 특수(가이아 파워→bowl2) 후 플레이어별로:

1. **팩션 기본 수입** (대부분 ore 1 + knowledge 1, 팩션별 차이)
2. **라운드 부스터 수입** (POWER_4 → 파워 차징, ECONOMY → credit 4 …)
3. **경제 트랙** — 레벨 1~5 → credit +4..+12 (옵션 A) / +2..+6 (옵션 B)
4. **과학 트랙** — 레벨 1~5 → knowledge +1..+5
5. **기술 타일 특수 수입**
6. **PI / 아카데미 수입** — 팩션별 보너스 (제노스 +QIC, 글린 +ore, 테란은 가이아 파워→bowl2)

### 5.3 PowerLeechService — 리치 메커니즘
건물 배치 후 2거리 이내 상대 플레이어별로:
1. 최대 파워값 = `max(건물 파워값) + ring(+2) + 본인 행성 매안(+1)`
2. 순환 가능 파워 = `bowl1×2 + bowl2×1` (타클론 브레인스톤 가산)
3. `effectivePower = min(리치, 순환 가능)`
4. `effectivePower == 1` 이고 *아이타 / 타클론 PI 가 아님* → **자동 수령**
5. 그 외 → `GameLeechOffer (status=PENDING)` 생성, `LEECH_OFFERED` 브로드캐스트 (동시 결정 배치)
6. VP 비용 = 1 per power (타클론 PI 는 bowl2 토큰 제거만, VP 0)

### 5.4 PowerActionService · 파워 액션
기본 7종 + 함대 12종. `GamePowerActionUsage` 에 `(gameId, round, actionCode)` 기록 — 라운드 내 1회 제한.

### 5.5 PassService
- 현재 부스터 VP + 고급 타일 패스 VP 지급 (건물 수, 행성 종류 등 조건)
- 현재 부스터 → offer 풀 반환, 새 부스터 선택 (R6 제외)
- `GamePlayerPass` 저장
- 모든 플레이어 패스되면 라운드 종료 → `IncomeService.applyRoundIncome` 호출 후 다음 라운드

### 5.6 FederationFormService
- 3+ 건물 연결 클러스터 검증, 파워값 합 계산
- 연방 타일 보상: `파워값 4→4파+토큰`, `6→6파+토큰`, `8→8파+토큰`, `10→10파+토큰`
- 엠바스 특수: 토큰 위치 자유 배치 + 재배치 가능

### 5.7 CommitTurnService (C안)
- FE가 계산한 `PlayerStateSnapshot` 을 서버가 **규칙 재검증 없이** `applySnapshot()` 으로 덮어씀
- `newBuildings`, `upgradedBuildings`, `hexChanges`, `newTechTiles`, `coveredTiles`, `newFederationGroups`, `newArtifacts`, `actionLog` 일괄 처리
- 다음 턴 이동 + 전체 `STATE_UPDATED` 브로드캐스트
- 단일 호출로 턴을 종결 → 왕복 대기/동시성 이슈 감소

### 5.8 GameWebSocketService — 브로드캐스트 이벤트
`/topic/room/{roomId}` 하나를 구독하면 모든 방 이벤트를 받는다. 주요 이벤트:

`PLAYER_JOINED · SEAT_CLAIMED · BOOSTER_SELECTED · GAME_STARTED · MINE_PLACED · STATE_UPDATED · TURN_CHANGED · PLAYER_PASSED · ROUND_STARTED · LEECH_OFFERED · LEECH_DECIDED · MAP_ROTATED · BIDDING_STARTED · BID_UPDATED · ITARS_GAIA_CHOICE · TINKEROIDS_ACTION_CHOICE · TERRANS_GAIA_CHOICE · POWER_INCOME_CHOICE · GAME_FINISHED · ACTION_LOGGED · VIEWER_COUNT`

페이로드: `{ roomId, eventType, timestamp, payload }` — `STATE_UPDATED` 는 `GameSnapshot` 전체.

---

## 6. REST API (컨트롤러)

| 컨트롤러 | 주요 엔드포인트 |
|---|---|
| **RoomController** | `POST /api/rooms` (생성), `POST /api/rooms/{id}/enter`, `GET /api/rooms/{id}/public-state`, `GET /api/rooms/code/{code}`, `POST /api/rooms/{id}/start-bidding`, `POST /api/rooms/{id}/seats/{no}/claim`, `POST /api/rooms/{id}/boosters/select`, `POST /api/rooms/{id}/start`, `POST /api/rooms/{id}/next-round`, `GET /api/rooms/{id}/players` |
| **ActionController** | `POST /api/rooms/{id}/actions/commit-turn` **(C안)**, `POST .../mine`, `.../upgrade`, `.../power`, `.../burn-power`, `.../tech-advance`, `.../deploy-gaiaformer`, `.../fleet-ship`, `.../fleet-probe`, `.../tech-tile-action`, `.../lost-planet` |
| **BuildingController** | `POST .../buildings/initial-mine` 등 |
| **PassController** | `POST /api/rooms/{id}/actions/pass` |
| **TechController** | `GET /api/rooms/{id}/tech` |
| **FederationController** | 검증·형성 API |
| **FleetController / LeechController / BiddingController** | 함대 / 파워 리치 / 비딩 |
| **RoundBooterController** | `GET /api/round/{id}/booster` 등 |
| **ScoringController** | 점수 / 최종 결과 |
| **MapController** | `GET .../map/hexes`, `POST .../map/sectors/{pos}/rotate` |
| **HealthController** | 헬스 체크 |

---

## 7. WebSocket 구성

`config/WebSocketConfig.java`
- Endpoint: `/ws` (SockJS fallback 포함)
- Broker: Simple In-Memory (`/topic` prefix)
- Pub prefix: `/app`
- CORS: `*` (개발용)

`config/WebSocketEventListener.java` — 연결/해제 로깅, 접속자 수(`VIEWER_COUNT`) 브로드캐스트.

---

## 8. 데이터베이스 — Flyway

### V1 (`V1__init_schema.sql`) — 약 35개 테이블
| 테이블 | 역할 |
|---|---|
| `game` | 세션 (status, room_code, phase, round, economy_track_option, bidding 필드) |
| `game_seat` | 좌석 1~4 (faction_type, player_id, turn_order) |
| `game_participant` | 입장 기록 (rejoin_token, claimed_seat_no) |
| `game_player_state` | 자원 / 파워 / 기술 트랙 / 건물 재고 / VP |
| `game_building` | 건물 배치 (hex_q, hex_r, type, ring) |
| `game_hex` | 맵 헥스 (planet_type, sector_id, position_no) |
| `game_action` | 액션 로그 |
| `game_player_pass` | 라운드별 패스 |
| `game_bid` | 비딩 이력 |
| `game_booster_offer`, `game_player_round_booster` | 부스터 풀/소유 |
| `game_leech_offer` | 파워 리치 배치 (batch_key, trigger/receive, vp_cost, status) |
| `game_tech_offer`, `game_adv_tech_offer`, `game_player_tech_tile` | 기본/고급 타일 |
| `game_federation_offer`, `game_federation_group/_building/_token_hex`, `game_player_federation_token` | 연방 |
| `game_artifact_offer`, `game_player_artifact` | 아티팩트 |
| `game_player_fleet_probe` | 함대 탐사선 |
| `game_round_scoring`, `game_final_scoring` | 라운드/최종 미션 |
| `game_vp_log` | VP 변동 이력 |
| `game_power_action_usage` | 파워 액션 라운드 1회 제한 |
| `player`, `registered_player` | 플레이어 메타 |

전 테이블 **UUID PK** + `@Version` 낙관적 락.

### V2 (`V2__auto_populate_nickname.sql`)
INSERT 트리거로 `game_seat` / `game_player_state` 에 닉네임 자동 채움 (디버깅 편의).

---

## 9. 게임 흐름 & 페이즈 전환

```
READY (방 생성 / 4명 입장 전)
 ↓ 4명 입장
MAP_ROTATE            — 맵 섹터 회전 가능
 ↓ startBidding()
BIDDING → BID_SEAT_PICK — 비딩으로 좌석 결정 (3라운드)
 ↓
SETUP_MINE_FIRST → SETUP_MINE_SECOND → (SETUP_MINE_XENOS) → (SETUP_MINE_EXPANSION)
    snake draft 1→2→3→4→4→3→2→1, XENOS / IVITS / TINKEROIDS / MOWEIDS 특수 분기
 ↓
BOOSTER_SELECTION     — 4→3→2→1 역순 부스터 픽
 ↓ startGame()
PLAYING (라운드 1~6)
   ├─ 라운드 시작 시 POWER_INCOME_CHOICE / ITARS_GAIA_CHOICE / TERRANS_GAIA_CHOICE / TINKEROIDS_ACTION_CHOICE 분기
   ├─ IncomeService.applyRoundIncome()
   ├─ 턴 진행 (commit-turn 또는 개별 액션)
   ├─ 파워 리치 동시 결정
   ├─ 모두 패스 → 라운드 미션 점수, 다음 라운드 준비
   └─ R6 종료 → 최종 미션 점수 + 비딩 패널티 → status = FINISHED
```

---

## 10. 핵심 비즈니스 로직 정리

### 10.1 파워 토큰 순환
- 차징(`chargePower(N)`): bowl1→bowl2 먼저, 남으면 bowl2→bowl3
- 소각(`burnPower()`): `bowl2 -2, bowl3 +1` (아이타는 가이아 +1)
- 가이아 포밍(`spendPowerToGaia`): bowl1→2→3 순서로 `gaiaPower` 이동
- 복귀 시 테란만 bowl2 로, 나머지는 bowl1로
- 네블라 PI 특수: bowl3 토큰 1개 = 파워 2 가치 → 사용시 `(amount+1)/2` 토큰 소비
- 타클론 브레인스톤: bowl3 있으면 최대 3파 충당

### 10.2 테라포밍 비용 (레벨별)
| techGaia | 필요 파워 |
|---|---|
| 0, 1 | 3 |
| 2 | 2 |
| 3, 4, 5 | 1 |

GAIA 행성에만 가이아포머 배치.

### 10.3 건물 업그레이드 비용 (기본)
- MINE → TRADING_STATION: 6 credit + 2 ore (인접 상대 있으면 3 credit)
- TS → RESEARCH_LAB: 5 credit + 3 ore
- RL → PI: 6 credit + 4 ore
- TS → ACADEMY: 6 credit + 6 ore

업그레이드는 리치 트리거.

### 10.4 연방 타일 보상
- 파워값 4 / 6 / 8 / 10 → 각각 4파 / 6파 / 8파 / 10파 충전 + 연방 토큰
- 엠바스만 토큰 위치 재배치 가능

### 10.5 점수 계산
- **라운드 미션**: 각 라운드마다 1개 조건 타일, 조건 충족 플레이어에게 1~6 VP
- **최종 미션 3장**: 조건 만족도 상위 플레이어에게 차등 VP
- **비딩 패널티**: 1순위 -2 VP, 2순위 -1 VP

---

## 11. 팩션별 특수 규칙 요약

| 팩션 | 핵심 특수 |
|---|---|
| TAKLONS | 브레인스톤 bowl3=3파, PI 전까지 리치 VP 0 (bowl2 토큰만 소모) |
| GLEENS | PI(=QIC 아카데미) 전까지 QIC → ore 자동 변환, 라운드 1회 QIC 아카데미 액션 |
| BAL_TAKS | PI 전까지 항법 트랙 잠김, 가이아포머 ↔ QIC 변환 (다음 라운드 반환) |
| IVITS | 초기 광산 대신 PI 배치, Space Station 건물 |
| TINKEROIDS | PI 배치 시작, 액션 코드 시스템 (라운드마다 1개 선택, 게임 전체에서 각 1회) |
| ITARS | 파워 소각시 gaiaPower +1, 리치 1파 거절 가능 |
| MOWEIDS | 건물 RING 씌우기 (파워값 +2), 초기 지식 +2 |
| NEVLAS | PI 이후 파워 2배 효율 |
| TERRANS | 가이아 복귀 시 bowl2 로 (라운드 시작 시 IncomeService가 처리) |
| BESCODS | 라운드 시작마다 최저 기술 트랙 1단계 자동 전진 |
| AMBAS | PI 이후 자신 건물 2개 위치 교환 |
| XENOS | 초기 광산 +1 (SETUP_MINE_XENOS 페이즈) |
| HADSCH_HALLAS | 기본 수입 credit +3 |
| GEODENS | 테라포밍 트랙 +1 스타트, 기본 수입 ore +1 |
| FIRAKS | 기본 수입 knowledge +1 |
| LANTIDS | 타인 건물 위에 무료 광산 (`is_lantids_mine = true`) |
| SPACE_GIANTS | PI 수입 +6파 차징 |
| DAKANIANS | 항법 +1 경제 +1 스타트, 초기 ore +2 |

---

## 12. 관찰된 아키텍처 강점과 한계

### 강점
1. **계층화된 책임 분리** — 컨트롤러 → 서비스(28개) → 리포지토리(40+) → 엔티티(45+)
2. **페이즈 머신 명시화** — `gamePhase` 문자열로 모든 상태 전이를 한 곳에서 추적
3. **C안 commit-turn** — FE가 한 번에 턴 결과를 스냅샷으로 송신 → 왕복 지연·동시성 부담 감소
4. **WebSocket 단일 토픽** — 구독 복잡도 낮음, `eventType` 기반 분기
5. **Flyway 중심 DDL** — `ddl-auto: none` 으로 스키마 재현 가능
6. **@Version 낙관적 락 + UUID PK** — 충돌/중복 방어

### 한계 / 리스크
1. **commit-turn 규칙 재검증 부재** — 서버가 FE를 신뢰. 변조 시 방어막이 없음
2. **In-Memory STOMP 브로커** — 멀티 인스턴스 확장 불가 (Redis/RabbitMQ 필요)
3. **GameSnapshot 재조립 비용** — 상태 변경마다 전체 스냅샷 브로드캐스트
4. **리치 동시성** — 복수 플레이어의 동시 결정은 트랜잭션 격리에 따라 미세 경합 가능
5. **테스트 커버리지** — 서비스 내부 `test.java` 등 아티팩트 정리 필요

---

## 13. 한눈에 보는 호출 경로 — "광산 배치" 예시

```
FE (commit-turn 요청)
  → ActionController#commitTurn
  → CommitTurnService
      ├─ GamePlayerState.applySnapshot(payload.playerState)
      ├─ newBuildings → GameBuilding 저장
      ├─ actionLog → GameAction insert
      ├─ PowerLeechService.createBatchAndProcess()   // 2거리 리치 대상
      │     ├─ 1파 자동 수령 (자동 파워 충전)
      │     └─ 그 외 → GameLeechOffer PENDING + LEECH_OFFERED WS
      ├─ RoundScoringService.checkMineScored()      // 라운드 미션 조건
      └─ ActionService.advanceTurnAndBroadcast()
            ├─ currentTurnSeatNo → 다음 플레이어
            └─ TURN_CHANGED + STATE_UPDATED WS 브로드캐스트
```

---

## 14. 결론

`gaia-project` 백엔드는 보드게임 규칙이 갖는 **높은 상태 의존성**과 **동시 결정 이벤트**를 다음 세 축으로 풀어낸다.

- **JPA 엔티티 = 게임 보드** (한 게임 ≒ 수십 개 테이블의 일관된 그래프)
- **서비스 = 규칙 엔진** (페이즈별·팩션별 분기를 명시적으로 계산)
- **WebSocket = 실시간 동기화** (단일 토픽, 스냅샷 이벤트, 동시 결정 배치)

FE와의 계약은 **개별 액션 API (레거시)** 와 **commit-turn (C안)** 두 가지를 병행하며, 후자가 표준 경로다. 4인 1세션 단기 플레이에는 충분히 견고하나, 멀티 인스턴스 확장·서버측 재검증·스냅샷 크기 최적화는 앞으로의 개선 여지다.
