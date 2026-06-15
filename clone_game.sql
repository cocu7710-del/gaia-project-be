-- 게임 1677bd71-68a6-455c-ad56-9cc487e9bc0c 를 동일 셋팅으로 새 게임 복제
-- R1 시작 상태 (셋업 종료 + R1 수입 적용 완료, 액션은 아직 안한 상태)

DO $$
DECLARE
    src_game UUID := '1677bd71-68a6-455c-ad56-9cc487e9bc0c';
    new_game UUID := gen_random_uuid();
    new_room_code VARCHAR(12);

    p_ambas UUID := 'c55ec360-e75a-482a-95c8-ca5b201a0ad2';
    p_hadsch UUID := 'a00e399c-73ee-444c-97e4-a371b419b8c0';
    p_terrans UUID := 'd0b5b5a1-c2e5-4004-93b3-3c7170ee7d59';
    p_bescods UUID := '7662b770-26b6-43f5-9e36-7c984fbab076';
BEGIN
    -- 새 room_code (8자 랜덤)
    new_room_code := upper(substring(replace(gen_random_uuid()::text, '-', '') for 8));

    RAISE NOTICE '새 게임 ID: %, room_code: %', new_game, new_room_code;

    -- 1. game 복제
    INSERT INTO game (id, status, title, room_code, max_players, current_round, current_turn_seat_no,
                      created_at, updated_at, version, economy_track_option, game_phase,
                      setup_mine_index, setup_mine_order,
                      tinkeroids_extra_ring_planet, moweids_extra_ring_planet,
                      bidding_round, bidding_current_bid, bidding_turn_player_id,
                      common_adv_tile_condition)
    SELECT new_game, 'IN_PROGRESS', title, new_room_code, max_players, 1, 1,
           NOW(), NOW(), 0, economy_track_option, 'PLAYING',
           setup_mine_index, setup_mine_order,
           tinkeroids_extra_ring_planet, moweids_extra_ring_planet,
           0, 0, NULL,
           common_adv_tile_condition
    FROM game WHERE id = src_game;

    -- 2. game_seat 복제
    INSERT INTO game_seat (id, game_id, seat_no, turn_order, faction_type, player_id, joined_at, nickname)
    SELECT gen_random_uuid(), new_game, seat_no, turn_order, faction_type, player_id, NOW(), nickname
    FROM game_seat WHERE game_id = src_game;

    -- 3. game_hex 복제 (composite PK, no id column)
    INSERT INTO game_hex (game_id, hex_q, hex_r, planet_type, sector_id, position_no)
    SELECT new_game, hex_q, hex_r, planet_type, sector_id, position_no
    FROM game_hex WHERE game_id = src_game;

    -- 4. game_sector_placement 복제
    INSERT INTO game_sector_placement (game_id, position_no, sector_id, rotation)
    SELECT new_game, position_no, sector_id, rotation
    FROM game_sector_placement WHERE game_id = src_game;

    -- 5. game_single_hex_placement 복제
    INSERT INTO game_single_hex_placement (game_id, position_no, tile_type)
    SELECT new_game, position_no, tile_type
    FROM game_single_hex_placement WHERE game_id = src_game;

    -- 6. game_round_scoring 복제
    INSERT INTO game_round_scoring (id, game_id, round_number, scoring_tile_code, created_at)
    SELECT gen_random_uuid(), new_game, round_number, scoring_tile_code, NOW()
    FROM game_round_scoring WHERE game_id = src_game;

    -- 7. game_final_scoring 복제
    INSERT INTO game_final_scoring (id, game_id, position, scoring_tile_code, created_at)
    SELECT gen_random_uuid(), new_game, position, scoring_tile_code, NOW()
    FROM game_final_scoring WHERE game_id = src_game;

    -- 8. game_booster_offer 복제 (선택 상태 그대로 — 같은 부스터 같은 사람)
    INSERT INTO game_booster_offer (id, game_id, position, booster_code, picked_by_seat_no,
                                    created_at, updated_at, version, taken_by_player_id)
    SELECT gen_random_uuid(), new_game, position, booster_code, picked_by_seat_no,
           NOW(), NOW(), 0, taken_by_player_id
    FROM game_booster_offer WHERE game_id = src_game;

    -- 9. game_tech_offer 복제 (taken_by_player_id 초기화 — 새 게임은 아직 아무도 안 가져감)
    INSERT INTO game_tech_offer (id, game_id, position, tech_track, tech_tile_code,
                                 created_at, updated_at, version, taken_by_player_id)
    SELECT gen_random_uuid(), new_game, position, tech_track, tech_tile_code,
           NOW(), NOW(), 0, NULL
    FROM game_tech_offer WHERE game_id = src_game;

    -- 10. game_adv_tech_offer 복제 (taken_by 초기화)
    INSERT INTO game_adv_tech_offer (id, game_id, position, tech_track, adv_tech_tile_code,
                                     created_at, updated_at, version, taken_by_player_id)
    SELECT gen_random_uuid(), new_game, position, tech_track, adv_tech_tile_code,
           NOW(), NOW(), 0, NULL
    FROM game_adv_tech_offer WHERE game_id = src_game;

    -- 11. game_artifact_offer 복제 (acquired_by 초기화)
    INSERT INTO game_artifact_offer (id, game_id, position, artifact_type, acquired_by, created_at)
    SELECT gen_random_uuid(), new_game, position, artifact_type, NULL, NOW()
    FROM game_artifact_offer WHERE game_id = src_game;

    -- 12. game_federation_offer 복제
    INSERT INTO game_federation_offer (id, game_id, federation_tile_type, quantity, position,
                                       created_at, updated_at, version)
    SELECT gen_random_uuid(), new_game, federation_tile_type, quantity, position,
           NOW(), NOW(), 0
    FROM game_federation_offer WHERE game_id = src_game;

    -- 13. game_player_round_booster 복제
    INSERT INTO game_player_round_booster (id, game_id, player_id, round_booster_type, selected_at)
    SELECT gen_random_uuid(), new_game, player_id, round_booster_type, NOW()
    FROM game_player_round_booster WHERE game_id = src_game;

    -- 14. game_participant 복제 (새 rejoin_token 생성)
    INSERT INTO game_participant (id, game_id, player_id, entered_at, claimed_seat_no, rejoin_token)
    SELECT gen_random_uuid(), new_game, player_id, NOW(), claimed_seat_no,
           upper(substring(replace(gen_random_uuid()::text, '-', '') for 32))
    FROM game_participant WHERE game_id = src_game;

    -- 15. game_bid 복제 (기록용)
    INSERT INTO game_bid (id, game_id, player_id, bid_round, bid_amount, is_passed, pick_order, seat_no, created_at)
    SELECT gen_random_uuid(), new_game, player_id, bid_round, bid_amount, is_passed, pick_order, seat_no, NOW()
    FROM game_bid WHERE game_id = src_game;

    -- 16. game_player_state — R1 시작 시점 자원 (수입 적용 완료, 액션 안한 상태)
    -- AMBAS (BOOSTER_14): 종족초기 + 부스터(c+2) + 종족수입(o+2,k+1) + 광산수입(o+2)
    INSERT INTO game_player_state (id, game_id, player_id, seat_no, ore, credit, knowledge, qic,
        power_bowl_1, power_bowl_2, power_bowl_3, victory_points, brainstone_bowl,
        tech_terraforming, tech_navigation, tech_ai, tech_gaia, tech_economy, tech_science,
        stock_mine, stock_trading_station, stock_research_lab, stock_planetary_institute,
        stock_academy, stock_gaiaformer, faction_type, gaia_power, federation_count, bid_penalty)
    VALUES
        (gen_random_uuid(), new_game, p_ambas, 4,
         8, 17, 4, 2, 2, 4, 0, 10, NULL,
         0, 1, 0, 0, 0, 0,
         6, 4, 3, 1, 2, 0, 'AMBAS', 0, 0, 0),
        (gen_random_uuid(), new_game, p_hadsch, 3,
         7, 20, 4, 1, 0, 5, 1, 10, NULL,
         0, 0, 0, 0, 1, 0,
         6, 4, 3, 1, 2, 0, 'HADSCH_HALLAS', 0, 0, 0),
        (gen_random_uuid(), new_game, p_terrans, 2,
         7, 15, 4, 1, 0, 8, 0, 10, NULL,
         0, 0, 0, 1, 0, 0,
         6, 4, 3, 1, 2, 1, 'TERRANS', 0, 0, 0),
        (gen_random_uuid(), new_game, p_bescods, 1,
         8, 15, 4, 1, 2, 4, 0, 10, NULL,
         0, 0, 0, 0, 0, 0,
         6, 4, 3, 1, 2, 0, 'BESCODS', 0, 0, 0);

    -- 17. 셋업 광산 (R1에 변경된 건물 제외)
    -- AMBAS: (7,-9), (10,-9)  [(9,-9)는 R1 BOOSTER_14 보너스라 제외]
    -- BESCODS: (-1,-3), (5,-6)
    -- HADSCH: (8,-6), (0,3)  [(0,3)는 현재 RL이지만 셋업은 MINE]
    -- TERRANS: (8,-9), (1,3)  [(1,3)는 현재 RL이지만 셋업은 MINE]
    INSERT INTO game_building (id, game_id, player_id, hex_q, hex_r, building_type, is_lantids_mine, academy_type, has_ring)
    VALUES
        (gen_random_uuid(), new_game, p_ambas,    7, -9, 'MINE', false, NULL, false),
        (gen_random_uuid(), new_game, p_ambas,   10, -9, 'MINE', false, NULL, false),
        (gen_random_uuid(), new_game, p_bescods, -1, -3, 'MINE', false, NULL, false),
        (gen_random_uuid(), new_game, p_bescods,  5, -6, 'MINE', false, NULL, false),
        (gen_random_uuid(), new_game, p_hadsch,   8, -6, 'MINE', false, NULL, false),
        (gen_random_uuid(), new_game, p_hadsch,   0,  3, 'MINE', false, NULL, false),
        (gen_random_uuid(), new_game, p_terrans,  8, -9, 'MINE', false, NULL, false),
        (gen_random_uuid(), new_game, p_terrans,  1,  3, 'MINE', false, NULL, false);

    -- 18. BASE VP 로그 (4명 각각 +10)
    INSERT INTO game_vp_log (id, game_id, player_id, category, amount, round_number, description, created_at)
    VALUES
        (gen_random_uuid(), new_game, p_ambas,   'BASE', 10, NULL, '기본 시작 VP', NOW()),
        (gen_random_uuid(), new_game, p_hadsch,  'BASE', 10, NULL, '기본 시작 VP', NOW()),
        (gen_random_uuid(), new_game, p_terrans, 'BASE', 10, NULL, '기본 시작 VP', NOW()),
        (gen_random_uuid(), new_game, p_bescods, 'BASE', 10, NULL, '기본 시작 VP', NOW());

    RAISE NOTICE '복제 완료. 새 game_id=%, room_code=%', new_game, new_room_code;
END $$;
