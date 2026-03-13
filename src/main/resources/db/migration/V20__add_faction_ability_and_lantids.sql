-- game_building: 란티다 전용 광산 여부
ALTER TABLE game_building
    ADD COLUMN is_lantids_mine BOOLEAN NOT NULL DEFAULT FALSE;

-- game_player_state: 종족 고유 능력 사용 여부 (라운드당 1회)
ALTER TABLE game_player_state
    ADD COLUMN faction_ability_used BOOLEAN NOT NULL DEFAULT FALSE;

-- game_player_state: 발타크 변환된 가이아포머 수
ALTER TABLE game_player_state
    ADD COLUMN baltaks_converted_gaiaformers INT NOT NULL DEFAULT 0;
