-- turn_order 초기값 0(미패스)을 허용하도록 체크 제약조건 수정
ALTER TABLE game_seat DROP CONSTRAINT IF EXISTS chk_turn_order_range;
ALTER TABLE game_seat ADD CONSTRAINT chk_turn_order_range CHECK (turn_order BETWEEN 0 AND 4);
