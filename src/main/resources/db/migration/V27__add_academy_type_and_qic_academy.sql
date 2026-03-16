-- game_building: 아카데미 종류 (KNOWLEDGE / QIC)
ALTER TABLE game_building ADD COLUMN academy_type VARCHAR(20) DEFAULT NULL;

-- game_player_state: QIC 아카데미 액션 사용 여부 (라운드당 1회)
ALTER TABLE game_player_state ADD COLUMN qic_academy_action_used BOOLEAN NOT NULL DEFAULT FALSE;
