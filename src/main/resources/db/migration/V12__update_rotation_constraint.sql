-- 기존 rotation constraint 삭제 후 60도 단위로 재생성
ALTER TABLE game_sector_placement DROP CONSTRAINT IF EXISTS chk_rotation;
ALTER TABLE game_sector_placement ADD CONSTRAINT chk_rotation CHECK (rotation IN (0, 60, 120, 180, 240, 300));
