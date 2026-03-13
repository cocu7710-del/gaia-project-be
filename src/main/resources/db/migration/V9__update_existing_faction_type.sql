-- 기존 플레이어 상태의 faction_type이 NULL인 경우 game_seat에서 복사
UPDATE game_player_state gps
SET faction_type = (
    SELECT gs.faction_type FROM game_seat gs
    WHERE gs.game_id = gps.game_id AND gs.seat_no = gps.seat_no
)
WHERE gps.faction_type IS NULL;
