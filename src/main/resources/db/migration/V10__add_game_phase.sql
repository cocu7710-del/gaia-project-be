-- 게임 페이즈 및 초기 광산 배치 인덱스 추가
ALTER TABLE game ADD COLUMN IF NOT EXISTS game_phase VARCHAR(30);
ALTER TABLE game ADD COLUMN IF NOT EXISTS setup_mine_index INT;

COMMENT ON COLUMN game.game_phase IS '게임 페이즈 (SETUP_MINE_FIRST, SETUP_MINE_SECOND, PLAYING)';
COMMENT ON COLUMN game.setup_mine_index IS '초기 광산 배치 인덱스 (0~7: 1→2→3→4→4→3→2→1 순서)';
