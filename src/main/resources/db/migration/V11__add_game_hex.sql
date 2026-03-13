-- 게임 헥스 테이블 (글로벌 좌표 + 행성 타입)
CREATE TABLE IF NOT EXISTS game_hex (
    game_id UUID NOT NULL REFERENCES game(id) ON DELETE CASCADE,
    hex_q INT NOT NULL,
    hex_r INT NOT NULL,
    planet_type VARCHAR(30) NOT NULL,
    sector_id VARCHAR(30),
    position_no INT,

    PRIMARY KEY (game_id, hex_q, hex_r)
);

COMMENT ON TABLE game_hex IS '게임 헥스 정보 (글로벌 좌표)';
COMMENT ON COLUMN game_hex.game_id IS '게임 ID';
COMMENT ON COLUMN game_hex.hex_q IS '헥스 Q 좌표 (글로벌)';
COMMENT ON COLUMN game_hex.hex_r IS '헥스 R 좌표 (글로벌)';
COMMENT ON COLUMN game_hex.planet_type IS '행성 타입';
COMMENT ON COLUMN game_hex.sector_id IS '섹터 ID (SECTOR_1, DEEP_SECTOR_1_FRONT 등)';
COMMENT ON COLUMN game_hex.position_no IS '맵 내 위치 번호';

CREATE INDEX IF NOT EXISTS ix_game_hex_game ON game_hex(game_id);
CREATE INDEX IF NOT EXISTS ix_game_hex_planet ON game_hex(game_id, planet_type);
