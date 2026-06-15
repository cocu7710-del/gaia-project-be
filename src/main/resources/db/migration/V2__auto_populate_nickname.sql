-- 플레이어 닉네임 비정규화 자동 채우기
-- INSERT/UPDATE 시 nickname이 NULL이면 player 테이블에서 조회해서 자동 세팅
-- BE 코드 수정 없이 디버깅 편의 확보

CREATE OR REPLACE FUNCTION fn_populate_nickname()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.nickname IS NULL AND NEW.player_id IS NOT NULL THEN
        SELECT nickname INTO NEW.nickname
          FROM player
         WHERE id = NEW.player_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 핵심 4개 테이블에 트리거 부착 (INSERT 시만, UPDATE는 무관 — nickname은 사실상 immutable)
CREATE TRIGGER trg_game_player_state_nickname
    BEFORE INSERT ON game_player_state
    FOR EACH ROW EXECUTE FUNCTION fn_populate_nickname();

CREATE TRIGGER trg_game_seat_nickname
    BEFORE INSERT OR UPDATE OF player_id ON game_seat
    FOR EACH ROW EXECUTE FUNCTION fn_populate_nickname();

CREATE TRIGGER trg_game_action_nickname
    BEFORE INSERT ON game_action
    FOR EACH ROW EXECUTE FUNCTION fn_populate_nickname();

CREATE TRIGGER trg_game_vp_log_nickname
    BEFORE INSERT ON game_vp_log
    FOR EACH ROW EXECUTE FUNCTION fn_populate_nickname();
