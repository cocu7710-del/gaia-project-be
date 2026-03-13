-- 재입장 토큰 컬럼 추가
ALTER TABLE game_participant ADD COLUMN rejoin_token VARCHAR(32);

-- 기존 데이터에 임시 토큰 생성
UPDATE game_participant SET rejoin_token = UPPER(SUBSTRING(REPLACE(gen_random_uuid()::text, '-', ''), 1, 8)) WHERE rejoin_token IS NULL;

-- NOT NULL 제약 조건 추가
ALTER TABLE game_participant ALTER COLUMN rejoin_token SET NOT NULL;
