-- 경제 트랙 옵션 컬럼 추가 (확장판)
ALTER TABLE game ADD COLUMN economy_track_option VARCHAR(20);

-- 기존 진행중인 게임은 기본값 OPTION_A로 설정
UPDATE game SET economy_track_option = 'OPTION_A' WHERE economy_track_option IS NULL AND status = 'IN_PROGRESS';
