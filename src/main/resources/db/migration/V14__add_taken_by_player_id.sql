-- 기술 타일에 획득자 컬럼 추가
ALTER TABLE game_tech_offer ADD COLUMN IF NOT EXISTS taken_by_player_id UUID;
ALTER TABLE game_adv_tech_offer ADD COLUMN IF NOT EXISTS taken_by_player_id UUID;
