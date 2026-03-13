-- game_player_tech_tile: ACTION 타입 기술 타일 사용 여부 (라운드당 1회)
ALTER TABLE game_player_tech_tile
    ADD COLUMN action_used BOOLEAN DEFAULT FALSE;
