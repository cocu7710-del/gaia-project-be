-- 모웨이드 PI: 건물에 링 씌우기 (파워값 +2)
ALTER TABLE game_building ADD COLUMN has_ring BOOLEAN NOT NULL DEFAULT FALSE;
