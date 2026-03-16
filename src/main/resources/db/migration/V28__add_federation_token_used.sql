-- 연방 토큰 사용(뒤집기) 여부 추가
ALTER TABLE game_federation_group ADD COLUMN used BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE game_player_federation_token ADD COLUMN used BOOLEAN NOT NULL DEFAULT FALSE;
