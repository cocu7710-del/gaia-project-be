CREATE TABLE game_leech_offer (
    id                 UUID         NOT NULL DEFAULT gen_random_uuid(),
    game_id            UUID         NOT NULL,
    batch_key          VARCHAR(36)  NOT NULL,
    trigger_player_id  UUID         NOT NULL,
    receive_player_id  UUID         NOT NULL,
    receive_seat_no    INT          NOT NULL,
    power_amount       INT          NOT NULL,
    vp_cost            INT          NOT NULL,
    status             VARCHAR(20)  NOT NULL,
    is_taklons         BOOLEAN      NOT NULL DEFAULT FALSE,
    sequence_no        INT          NOT NULL,
    taklons_choice     VARCHAR(20),
    follow_up_type     VARCHAR(50),
    follow_up_data     TEXT,
    created_at         TIMESTAMP    NOT NULL,
    decided_at         TIMESTAMP,
    CONSTRAINT pk_game_leech_offer PRIMARY KEY (id)
);
