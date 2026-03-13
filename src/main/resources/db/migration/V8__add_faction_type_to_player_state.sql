-- 플레이어 상태에 종족 타입 컬럼 추가
ALTER TABLE game_player_state ADD COLUMN faction_type VARCHAR(30);

-- 기존 데이터에 대해 game_seat에서 종족 정보 복사
UPDATE game_player_state gps
SET faction_type = (
    SELECT gs.faction_type FROM game_seat gs
    WHERE gs.game_id = gps.game_id AND gs.seat_no = gps.seat_no
)
WHERE faction_type IS NULL;
