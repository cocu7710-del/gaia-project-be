ALTER TABLE registered_player
    ADD COLUMN is_real_player BOOLEAN NOT NULL DEFAULT true;
