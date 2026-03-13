-- UUID 생성 함수
create extension if not exists pgcrypto;

-- ============================================================
-- 1) game
-- ============================================================
create table if not exists game (
    id uuid primary key default gen_random_uuid(),
    status varchar(30) not null,
    title varchar(100),
    room_code varchar(12),
    max_players int not null default 4,
    current_round int,
    current_turn_seat_no int,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    version bigint not null default 0
);

comment on table game is '게임(방) 정보';
comment on column game.id is '게임 ID';
comment on column game.status is '게임 상태 (READY/IN_PROGRESS/FINISHED)';
comment on column game.title is '방 제목';
comment on column game.room_code is '방 입장 코드';
comment on column game.max_players is '최대 플레이어 수';
comment on column game.current_round is '현재 라운드 (1~6)';
comment on column game.current_turn_seat_no is '현재 턴 좌석 번호 (1~4)';
comment on column game.created_at is '생성일시';
comment on column game.updated_at is '수정일시';
comment on column game.version is '낙관적 락 버전';

create unique index if not exists uq_game_room_code
    on game(room_code)
    where room_code is not null;

-- ============================================================
-- 2) player
-- ============================================================
create table if not exists player (
    id uuid primary key default gen_random_uuid(),
    nickname varchar(100) not null,
    created_at timestamp not null default now()
);

comment on table player is '플레이어 정보';
comment on column player.id is '플레이어 ID';
comment on column player.nickname is '닉네임';
comment on column player.created_at is '생성일시';

-- ============================================================
-- 3) game_participant (좌석 선택 전 '입장' 기록)
-- ============================================================
create table if not exists game_participant (
    id uuid primary key default gen_random_uuid(),
    game_id uuid not null references game(id) on delete cascade,
    player_id uuid not null references player(id) on delete cascade,
    entered_at timestamp not null default now(),
    claimed_seat_no int,

    constraint uq_game_participant unique (game_id, player_id),
    constraint chk_claimed_seat_range check (claimed_seat_no is null or (claimed_seat_no between 1 and 4))
);

comment on table game_participant is '게임 입장 기록 (좌석 선택 전)';
comment on column game_participant.id is '참가 기록 ID';
comment on column game_participant.game_id is '게임 ID';
comment on column game_participant.player_id is '플레이어 ID';
comment on column game_participant.entered_at is '입장 시각';
comment on column game_participant.claimed_seat_no is '선택한 좌석 번호 (1~4)';

-- ============================================================
-- 4) game_seat (게임 생성 시 1~4 생성, 종족/턴순서 확정)
-- ============================================================
create table if not exists game_seat (
    id uuid primary key default gen_random_uuid(),
    game_id uuid not null references game(id) on delete cascade,
    seat_no int not null,
    turn_order int not null,
    faction_type varchar(50) not null,
    player_id uuid references player(id),
    joined_at timestamp,

    constraint uq_game_seat unique (game_id, seat_no),
    constraint uq_game_turn_order unique (game_id, turn_order),
    constraint chk_seat_no_range check (seat_no between 1 and 4),
    constraint chk_turn_order_range check (turn_order between 1 and 4)
);

comment on table game_seat is '게임 좌석 정보 (종족/턴순서 확정)';
comment on column game_seat.id is '좌석 ID';
comment on column game_seat.game_id is '게임 ID';
comment on column game_seat.seat_no is '좌석 번호 (1~4)';
comment on column game_seat.turn_order is '턴 순서 (1~4)';
comment on column game_seat.faction_type is '종족 타입';
comment on column game_seat.player_id is '착석한 플레이어 ID';
comment on column game_seat.joined_at is '착석 시각';

create unique index if not exists uq_game_seat_player_not_null
    on game_seat(game_id, player_id)
    where player_id is not null;

-- ============================================================
-- 5) game_sector_placement (섹터 배치 레시피)
-- ============================================================
create table if not exists game_sector_placement (
    game_id uuid not null references game(id) on delete cascade,
    position_no int not null,
    sector_id varchar(20) not null,
    rotation int not null,

    primary key (game_id, position_no),
    constraint chk_rotation check (rotation in (0,30,60,90,120,150))
);

comment on table game_sector_placement is '섹터 배치 정보';
comment on column game_sector_placement.game_id is '게임 ID';
comment on column game_sector_placement.position_no is '보드 내 섹터 배치 위치 번호';
comment on column game_sector_placement.sector_id is '섹터 ID (S1~S10 등)';
comment on column game_sector_placement.rotation is '회전 각도 (0/60/120/180/240/300)';

-- ============================================================
-- 6) game_booster_offer (라운드 부스터 공개 목록: 인원수 + 3)
-- ============================================================
create table if not exists game_booster_offer (
    id uuid primary key default gen_random_uuid(),
    game_id uuid not null references game(id) on delete cascade,
    position int not null,
    booster_code varchar(50) not null,
    picked_by_seat_no int,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    version bigint not null default 0,

    constraint uq_game_booster_offer_pos unique (game_id, position),
    constraint uq_game_booster_offer_code unique (game_id, booster_code),
    constraint chk_picked_seat_range check (picked_by_seat_no is null or (picked_by_seat_no between 1 and 4))
);

comment on table game_booster_offer is '라운드 부스터 공개 목록';
comment on column game_booster_offer.id is '부스터 제공 ID';
comment on column game_booster_offer.game_id is '게임 ID';
comment on column game_booster_offer.position is '보드 내 부스터 위치';
comment on column game_booster_offer.booster_code is '부스터 코드 (RB01 등)';
comment on column game_booster_offer.picked_by_seat_no is '선택한 좌석 번호 (1~4, 미선택시 null)';
comment on column game_booster_offer.created_at is '생성일시';
comment on column game_booster_offer.updated_at is '수정일시';
comment on column game_booster_offer.version is '낙관적 락 버전';

create index if not exists ix_game_booster_offer_game
    on game_booster_offer(game_id);

-- ============================================================
-- 8) game_tech_offer (일반 기술타일 공개 목록)
-- ============================================================
create table if not exists game_tech_offer (
    id uuid primary key default gen_random_uuid(),
    game_id uuid not null references game(id) on delete cascade,
    position int not null,
    tech_track varchar(30) not null,
    tech_tile_code varchar(50) not null,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    version bigint not null default 0,

    constraint uq_game_tech_offer_pos unique (game_id, position),
    constraint uq_game_tech_offer_code unique (game_id, tech_tile_code)
);

comment on table game_tech_offer is '일반 기술타일 공개 목록';
comment on column game_tech_offer.id is '기술타일 제공 ID';
comment on column game_tech_offer.game_id is '게임 ID';
comment on column game_tech_offer.position is '게임판 기술타일 보드 위치 (1~9)';
comment on column game_tech_offer.tech_track is '기술 트랙 종류 (TERRAFORMING, NAVIGATION 등)';
comment on column game_tech_offer.tech_tile_code is '해당 위치에 놓인 기술타일 코드 (BASIC_TILE_1 등)';
comment on column game_tech_offer.created_at is '생성일시';
comment on column game_tech_offer.updated_at is '수정일시';
comment on column game_tech_offer.version is '낙관적 락 버전';

create index if not exists ix_game_tech_offer_game
    on game_tech_offer(game_id);

-- ============================================================
-- 9) game_adv_tech_offer (고급 기술타일 공개 목록)
-- ============================================================
create table if not exists game_adv_tech_offer (
    id uuid primary key default gen_random_uuid(),
    game_id uuid not null references game(id) on delete cascade,
    position int not null,
    tech_track varchar(30) not null,
    adv_tech_tile_code varchar(50) not null,
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    version bigint not null default 0,

    constraint uq_game_adv_offer_track unique (game_id, tech_track),
    constraint uq_game_adv_offer_code unique (game_id, adv_tech_tile_code)
);

comment on table game_adv_tech_offer is '고급 기술타일 공개 목록';
comment on column game_adv_tech_offer.id is '고급 기술타일 제공 ID';
comment on column game_adv_tech_offer.game_id is '게임 ID';
comment on column game_adv_tech_offer.position is '게임판 기술타일 보드 위치 (1~7)';
comment on column game_adv_tech_offer.tech_track is '기술 트랙 종류 (TERRAFORMING, NAVIGATION 등)';
comment on column game_adv_tech_offer.adv_tech_tile_code is '해당 트랙에 놓인 고급 기술타일 코드 (ADVANCED_TILE_1 등)';
comment on column game_adv_tech_offer.created_at is '생성일시';
comment on column game_adv_tech_offer.updated_at is '수정일시';
comment on column game_adv_tech_offer.version is '낙관적 락 버전';

create index if not exists ix_game_adv_tech_offer_game
    on game_adv_tech_offer(game_id);

-- ============================================================
-- 10) game_player_state (플레이어 게임 상태)
-- ============================================================
create table if not exists game_player_state (
    id uuid primary key default gen_random_uuid(),
    game_id uuid not null references game(id) on delete cascade,
    player_id uuid not null references player(id) on delete cascade,
    seat_no int not null,

    -- 자원
    ore int not null default 0,
    credit int not null default 0,
    knowledge int not null default 0,
    qic int not null default 0,

    -- 파워
    power_bowl_1 int not null default 0,
    power_bowl_2 int not null default 0,
    power_bowl_3 int not null default 0,

    -- VP
    victory_points int not null default 0,

    -- 브레인스톤 (타클론 전용, null: 없음, 1/2/3: 해당 bowl)
    brainstone_bowl int,

    -- 기술 트랙 레벨 (0~5)
    tech_terraforming int not null default 0,
    tech_navigation int not null default 0,
    tech_ai int not null default 0,
    tech_gaia int not null default 0,
    tech_economy int not null default 0,
    tech_science int not null default 0,

    -- 건물 재고
    stock_mine int not null default 8,
    stock_trading_station int not null default 4,
    stock_research_lab int not null default 3,
    stock_planetary_institute int not null default 1,
    stock_academy int not null default 2,
    stock_gaiaformer int not null default 0,

    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    version bigint not null default 0,

    constraint uq_game_player_state unique (game_id, player_id),
    constraint chk_brainstone_bowl check (brainstone_bowl is null or brainstone_bowl between 1 and 3)
);

comment on table game_player_state is '게임 내 플레이어 상태 (자원, VP 등)';
comment on column game_player_state.id is '플레이어 상태 ID';
comment on column game_player_state.game_id is '게임 ID';
comment on column game_player_state.player_id is '플레이어 ID';
comment on column game_player_state.seat_no is '좌석 번호 (1~4)';
comment on column game_player_state.ore is '광석 보유량';
comment on column game_player_state.credit is '크레딧 보유량';
comment on column game_player_state.knowledge is '지식 보유량';
comment on column game_player_state.qic is 'QIC 보유량';
comment on column game_player_state.power_bowl_1 is '파워 Bowl I 토큰 수';
comment on column game_player_state.power_bowl_2 is '파워 Bowl II 토큰 수';
comment on column game_player_state.power_bowl_3 is '파워 Bowl III 토큰 수';
comment on column game_player_state.victory_points is '승점';
comment on column game_player_state.brainstone_bowl is '브레인스톤 위치 (타클론 전용, null: 없음, 1/2/3: 해당 bowl)';
comment on column game_player_state.tech_terraforming is '테라포밍 트랙 레벨 (0~5)';
comment on column game_player_state.tech_navigation is '항해 트랙 레벨 (0~5)';
comment on column game_player_state.tech_ai is 'AI 트랙 레벨 (0~5)';
comment on column game_player_state.tech_gaia is '가이아 트랙 레벨 (0~5)';
comment on column game_player_state.tech_economy is '경제 트랙 레벨 (0~5)';
comment on column game_player_state.tech_science is '과학 트랙 레벨 (0~5)';
comment on column game_player_state.stock_mine is '광산 재고';
comment on column game_player_state.stock_trading_station is '거래소 재고';
comment on column game_player_state.stock_research_lab is '연구소 재고';
comment on column game_player_state.stock_planetary_institute is '행성수도 재고';
comment on column game_player_state.stock_academy is '학원 재고';
comment on column game_player_state.stock_gaiaformer is '가이아포머 재고';
comment on column game_player_state.created_at is '생성일시';
comment on column game_player_state.updated_at is '수정일시';
comment on column game_player_state.version is '낙관적 락 버전';

-- ============================================================
-- 11) game_federation_offer (연방 타일 공개 목록)
-- ============================================================
create table if not exists game_federation_offer (
    id uuid primary key default gen_random_uuid(),
    game_id uuid not null references game(id) on delete cascade,
    federation_tile_type varchar(50) not null,
    quantity int not null,
    position int,  -- null: 일반 공급처, 1~4: 잊힌 함대, 0: 테라포밍 트랙
    created_at timestamp not null default now(),
    updated_at timestamp not null default now(),
    version bigint not null default 0,

    constraint uq_game_federation_offer unique (game_id, federation_tile_type, position)
);

comment on table game_federation_offer is '연방 타일 공개 목록';
comment on column game_federation_offer.id is '연방 타일 제공 ID';
comment on column game_federation_offer.game_id is '게임 ID';
comment on column game_federation_offer.federation_tile_type is '연방 타일 종류 (FED_TILE_1 등)';
comment on column game_federation_offer.quantity is '남은 개수';
comment on column game_federation_offer.position is '배치 위치 (null: 일반 공급처, 1~4: 잊힌 함대, 0: 테라포밍 트랙)';
comment on column game_federation_offer.created_at is '생성일시';
comment on column game_federation_offer.updated_at is '수정일시';
comment on column game_federation_offer.version is '낙관적 락 버전';

create index if not exists ix_game_federation_offer_game
    on game_federation_offer(game_id);

-- ============================================================
-- 12) game_player_tech_tile (플레이어 보유 기술 타일)
-- ============================================================
create table if not exists game_player_tech_tile (
    id uuid primary key default gen_random_uuid(),
    game_id uuid not null references game(id) on delete cascade,
    player_id uuid not null references player(id) on delete cascade,
    tech_tile_code varchar(50) not null,
    is_covered boolean default false not null,
    covered_by varchar(50),
    acquired_at timestamp not null default now(),
    covered_at timestamp,

    constraint uq_player_tech_tile unique (game_id, player_id, tech_tile_code)
);

comment on table game_player_tech_tile is '플레이어가 보유한 기술 타일 (기본/고급)';
comment on column game_player_tech_tile.id is '기술 타일 ID';
comment on column game_player_tech_tile.game_id is '게임 ID';
comment on column game_player_tech_tile.player_id is '플레이어 ID';
comment on column game_player_tech_tile.tech_tile_code is '기술 타일 코드 (BASIC_TILE_1, ADV_TILE_1 등)';
comment on column game_player_tech_tile.is_covered is '고급 타일에 덮혔는지 여부';
comment on column game_player_tech_tile.covered_by is '덮은 고급 타일 코드';
comment on column game_player_tech_tile.acquired_at is '획득 시각';
comment on column game_player_tech_tile.covered_at is '덮힌 시각';

create index if not exists ix_player_tech_tile_player
    on game_player_tech_tile(game_id, player_id);
create index if not exists ix_player_tech_tile_active
    on game_player_tech_tile(game_id, player_id, is_covered);

-- ============================================================
-- 13) game_player_federation_token (플레이어 연방 토큰)
-- ============================================================
create table if not exists game_player_federation_token (
    id uuid primary key default gen_random_uuid(),
    game_id uuid not null references game(id) on delete cascade,
    player_id uuid not null references player(id) on delete cascade,
    federation_tile_type varchar(50) not null,
    acquired_at timestamp not null default now()
);

comment on table game_player_federation_token is '플레이어가 획득한 연방 토큰';
comment on column game_player_federation_token.id is '연방 토큰 ID';
comment on column game_player_federation_token.game_id is '게임 ID';
comment on column game_player_federation_token.player_id is '플레이어 ID';
comment on column game_player_federation_token.federation_tile_type is '연방 타일 종류 (FED_TILE_1 등)';
comment on column game_player_federation_token.acquired_at is '획득 시각';

create index if not exists ix_player_federation_token_player
    on game_player_federation_token(player_id);
create index if not exists ix_player_federation_token_game_player
    on game_player_federation_token(game_id, player_id);

-- ============================================================
-- 14) game_player_round_booster (플레이어 라운드 부스터)
-- ============================================================
create table if not exists game_player_round_booster (
    id uuid primary key default gen_random_uuid(),
    game_id uuid not null references game(id) on delete cascade,
    player_id uuid not null references player(id) on delete cascade,
    round_booster_type varchar(50) not null,
    selected_at timestamp not null default now(),

    constraint uq_player_round_booster unique (game_id, player_id)
);

comment on table game_player_round_booster is '플레이어의 현재 라운드 부스터';
comment on column game_player_round_booster.id is '라운드 부스터 ID';
comment on column game_player_round_booster.game_id is '게임 ID';
comment on column game_player_round_booster.player_id is '플레이어 ID';
comment on column game_player_round_booster.round_booster_type is '라운드 부스터 종류 (BOOSTER_1 등)';
comment on column game_player_round_booster.selected_at is '선택 시각';

create index if not exists ix_player_round_booster_player
    on game_player_round_booster(player_id);

-- ============================================================
-- 15) game_player_artifact (플레이어 인공물)
-- ============================================================
create table if not exists game_player_artifact (
    id uuid primary key default gen_random_uuid(),
    game_id uuid not null references game(id) on delete cascade,
    player_id uuid not null references player(id) on delete cascade,
    artifact_type varchar(50) not null,
    acquired_at timestamp not null default now()
);

comment on table game_player_artifact is '플레이어가 획득한 인공물';
comment on column game_player_artifact.id is '인공물 ID';
comment on column game_player_artifact.game_id is '게임 ID';
comment on column game_player_artifact.player_id is '플레이어 ID';
comment on column game_player_artifact.artifact_type is '인공물 종류 (ARTIFACT_1 등)';
comment on column game_player_artifact.acquired_at is '획득 시각';

create index if not exists ix_player_artifact_player
    on game_player_artifact(player_id);
create index if not exists ix_player_artifact_game_player
    on game_player_artifact(game_id, player_id);

-- ============================================================
-- 16) game_player_fleet_probe (플레이어 함대 탐사선)
-- ============================================================
create table if not exists game_player_fleet_probe (
    id uuid primary key default gen_random_uuid(),
    game_id uuid not null references game(id) on delete cascade,
    player_id uuid not null references player(id) on delete cascade,
    fleet_name varchar(30) not null,
    placed_at timestamp not null default now(),

    constraint uq_player_fleet_probe unique (game_id, player_id, fleet_name)
);

comment on table game_player_fleet_probe is '플레이어 함대 탐사선 배치';
comment on column game_player_fleet_probe.id is '함대 탐사선 ID';
comment on column game_player_fleet_probe.game_id is '게임 ID';
comment on column game_player_fleet_probe.player_id is '플레이어 ID';
comment on column game_player_fleet_probe.fleet_name is '함대 이름 (TF_MARS, ECLIPSE, TWILIGHT, REBELLION)';
comment on column game_player_fleet_probe.placed_at is '배치 시각';

create index if not exists ix_player_fleet_probe_player
    on game_player_fleet_probe(player_id);
create index if not exists ix_player_fleet_probe_game_player
    on game_player_fleet_probe(game_id, player_id);

-- ============================================================
-- 17) game_power_action_usage (파워 액션 사용 추적)
-- ============================================================
create table if not exists game_power_action_usage (
    id uuid primary key default gen_random_uuid(),
    game_id uuid not null references game(id) on delete cascade,
    round_number int not null,
    power_action_type varchar(100) not null,
    player_id uuid not null references player(id),
    used_at timestamp not null default now(),

    constraint uq_game_power_action_usage unique (game_id, round_number, power_action_type)
);

comment on table game_power_action_usage is '파워 액션 사용 내역';
comment on column game_power_action_usage.id is '파워 액션 사용 ID';
comment on column game_power_action_usage.game_id is '게임 ID';
comment on column game_power_action_usage.round_number is '라운드 번호';
comment on column game_power_action_usage.power_action_type is '파워 액션 종류 (Enum)';
comment on column game_power_action_usage.player_id is '사용한 플레이어 ID';
comment on column game_power_action_usage.used_at is '사용 시각';

create index if not exists ix_game_power_action_game_round
    on game_power_action_usage(game_id, round_number);
create index if not exists ix_game_power_action_player
    on game_power_action_usage(player_id);

-- ============================================================
-- 18) game_artifact_offer (게임 인공물 공급)
-- ============================================================
create table if not exists game_artifact_offer (
    id uuid primary key default gen_random_uuid(),
    game_id uuid not null references game(id) on delete cascade,
    artifact_type varchar(50) not null,
    position int not null,
    is_acquired boolean default false not null,
    acquired_by uuid references player(id),
    acquired_at timestamp,
    created_at timestamp not null default now(),

    constraint uq_game_artifact_offer unique (game_id, artifact_type),
    constraint uq_game_artifact_position unique (game_id, position)
);

comment on table game_artifact_offer is '게임 인공물 공급 (선택된 인공물 목록)';
comment on column game_artifact_offer.id is '인공물 공급 ID';
comment on column game_artifact_offer.game_id is '게임 ID';
comment on column game_artifact_offer.artifact_type is '인공물 종류';
comment on column game_artifact_offer.position is '배치 위치 (1~6)';
comment on column game_artifact_offer.is_acquired is '획득 여부';
comment on column game_artifact_offer.acquired_by is '획득한 플레이어 ID';
comment on column game_artifact_offer.acquired_at is '획득 시각';
comment on column game_artifact_offer.created_at is '생성일시';

create index if not exists ix_game_artifact_offer_game
    on game_artifact_offer(game_id);

-- ============================================================
-- 19) game_round_scoring (라운드 점수 타일)
-- ============================================================
create table if not exists game_round_scoring (
    id uuid primary key default gen_random_uuid(),
    game_id uuid not null references game(id) on delete cascade,
    round_number int not null,
    scoring_tile_code varchar(50) not null,
    created_at timestamp not null default now(),

    constraint uq_game_round_scoring unique (game_id, round_number)
);

comment on table game_round_scoring is '라운드별 점수 타일';
comment on column game_round_scoring.id is '라운드 점수 ID';
comment on column game_round_scoring.game_id is '게임 ID';
comment on column game_round_scoring.round_number is '라운드 번호 (1~6)';
comment on column game_round_scoring.scoring_tile_code is '점수 타일 코드';
comment on column game_round_scoring.created_at is '생성일시';

create index if not exists ix_game_round_scoring_game
    on game_round_scoring(game_id);

-- ============================================================
-- 20) game_final_scoring (최종 점수 타일)
-- ============================================================
create table if not exists game_final_scoring (
    id uuid primary key default gen_random_uuid(),
    game_id uuid not null references game(id) on delete cascade,
    position int not null,
    scoring_tile_code varchar(50) not null,
    created_at timestamp not null default now(),

    constraint uq_game_final_scoring unique (game_id, position)
);

comment on table game_final_scoring is '최종 점수 타일';
comment on column game_final_scoring.id is '최종 점수 ID';
comment on column game_final_scoring.game_id is '게임 ID';
comment on column game_final_scoring.position is '위치 (1 또는 2)';
comment on column game_final_scoring.scoring_tile_code is '점수 타일 코드';
comment on column game_final_scoring.created_at is '생성일시';

create index if not exists ix_game_final_scoring_game
    on game_final_scoring(game_id);

-- ============================================================
-- 21) game_building (건물 배치)
-- ============================================================
create table if not exists game_building (
    id uuid primary key default gen_random_uuid(),
    game_id uuid not null references game(id) on delete cascade,
    player_id uuid not null references player(id) on delete cascade,
    hex_q int not null,
    hex_r int not null,
    building_type varchar(30) not null,

    constraint uq_game_building_hex unique (game_id, hex_q, hex_r)
);

comment on table game_building is '건물 배치 정보';
comment on column game_building.id is '건물 ID';
comment on column game_building.game_id is '게임 ID';
comment on column game_building.player_id is '소유 플레이어 ID';
comment on column game_building.hex_q is '헥스 Q 좌표';
comment on column game_building.hex_r is '헥스 R 좌표';
comment on column game_building.building_type is '건물 타입';

create index if not exists ix_game_building_game
    on game_building(game_id);
create index if not exists ix_game_building_player
    on game_building(game_id, player_id);

-- ============================================================
-- 22) game_single_hex_placement (1헥스 타일 배치)
-- ============================================================
create table if not exists game_single_hex_placement (
    game_id uuid not null references game(id) on delete cascade,
    position_no int not null,
    tile_type varchar(50) not null,

    primary key (game_id, position_no)
);

comment on table game_single_hex_placement is '1헥스 타일 배치 정보';
comment on column game_single_hex_placement.game_id is '게임 ID';
comment on column game_single_hex_placement.position_no is '배치 위치 번호';
comment on column game_single_hex_placement.tile_type is '타일 타입 (SingleHexTileType)';

create index if not exists ix_game_single_hex_placement_game
    on game_single_hex_placement(game_id);
