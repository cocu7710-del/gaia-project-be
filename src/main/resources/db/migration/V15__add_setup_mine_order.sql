-- 초기 광산 배치 순서 JSON 컬럼 추가
ALTER TABLE game ADD COLUMN IF NOT EXISTS setup_mine_order TEXT;

-- 기존 setup_mine_index 컬럼 코멘트 업데이트
COMMENT ON COLUMN game.setup_mine_index IS '초기 광산 배치 현재 인덱스 (배치 순서 배열의 위치)';
COMMENT ON COLUMN game.setup_mine_order IS '초기 광산 배치 순서 JSON 배열 (예: [1,2,3,4,2,3,2,1,4])';
