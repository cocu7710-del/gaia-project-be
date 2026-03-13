-- V16: 게임 액션 시스템 추가
-- 액션 기록, 패스 기록

-- GameBoosterOffer에 taken_by_player_id 컬럼 추가 (부스터 교체 시스템용)
ALTER TABLE game_booster_offer ADD COLUMN IF NOT EXISTS taken_by_player_id UUID;

-- 게임 액션 테이블 (확정된 액션만 저장)
CREATE TABLE game_action (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id UUID NOT NULL REFERENCES game(id) ON DELETE CASCADE,
    player_id UUID NOT NULL REFERENCES player(id) ON DELETE CASCADE,
    round_number INT NOT NULL,
    turn_sequence INT NOT NULL,
    action_type VARCHAR(50) NOT NULL,
    action_data TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 플레이어 패스 기록 테이블
CREATE TABLE game_player_pass (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    game_id UUID NOT NULL REFERENCES game(id) ON DELETE CASCADE,
    player_id UUID NOT NULL REFERENCES player(id) ON DELETE CASCADE,
    round_number INT NOT NULL,
    passed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_player_pass_round UNIQUE (game_id, player_id, round_number)
);

-- 인덱스 추가
CREATE INDEX idx_game_action_game_player ON game_action(game_id, player_id);
CREATE INDEX idx_game_action_round ON game_action(game_id, round_number);
CREATE INDEX idx_player_pass_round ON game_player_pass(game_id, round_number);
