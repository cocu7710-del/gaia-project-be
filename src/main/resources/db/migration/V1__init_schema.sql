--
--



SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

--
-- Name: pgcrypto; Type: EXTENSION; Schema: -; Owner: -
--

CREATE EXTENSION IF NOT EXISTS pgcrypto WITH SCHEMA public;


SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: game; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.game (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    status character varying(30) NOT NULL,
    title character varying(100),
    room_code character varying(12),
    max_players integer DEFAULT 4 NOT NULL,
    current_round integer,
    current_turn_seat_no integer,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    economy_track_option character varying(20),
    game_phase character varying(30),
    setup_mine_index integer,
    setup_mine_order text,
    tinkeroids_extra_ring_planet character varying(20),
    moweids_extra_ring_planet character varying(20),
    bidding_round integer DEFAULT 0 NOT NULL,
    bidding_current_bid integer DEFAULT 0 NOT NULL,
    bidding_turn_player_id uuid,
    common_adv_tile_condition character varying(20)
);


--
-- Name: game_action; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.game_action (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    game_id uuid NOT NULL,
    player_id uuid NOT NULL,
    round_number integer NOT NULL,
    turn_sequence integer NOT NULL,
    action_type character varying(50) NOT NULL,
    action_data text,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    nickname character varying(50)
);


--
-- Name: game_adv_tech_offer; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.game_adv_tech_offer (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    game_id uuid NOT NULL,
    "position" integer NOT NULL,
    tech_track character varying(30) NOT NULL,
    adv_tech_tile_code character varying(50) NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    taken_by_player_id uuid
);


--
-- Name: game_artifact_offer; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.game_artifact_offer (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    game_id uuid NOT NULL,
    artifact_type character varying(50) NOT NULL,
    "position" integer NOT NULL,
    is_acquired boolean DEFAULT false NOT NULL,
    acquired_by uuid,
    acquired_at timestamp without time zone,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: game_bid; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.game_bid (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    game_id uuid NOT NULL,
    player_id uuid NOT NULL,
    bid_round integer DEFAULT 1 NOT NULL,
    bid_amount integer DEFAULT 0 NOT NULL,
    is_passed boolean DEFAULT false NOT NULL,
    pick_order integer,
    seat_no integer,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: game_booster_offer; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.game_booster_offer (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    game_id uuid NOT NULL,
    "position" integer NOT NULL,
    booster_code character varying(50) NOT NULL,
    picked_by_seat_no integer,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    taken_by_player_id uuid,
    CONSTRAINT chk_picked_seat_range CHECK (((picked_by_seat_no IS NULL) OR ((picked_by_seat_no >= 1) AND (picked_by_seat_no <= 4))))
);


--
-- Name: game_building; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.game_building (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    game_id uuid NOT NULL,
    player_id uuid NOT NULL,
    hex_q integer NOT NULL,
    hex_r integer NOT NULL,
    building_type character varying(30) NOT NULL,
    is_lantids_mine boolean DEFAULT false NOT NULL,
    academy_type character varying(20) DEFAULT NULL::character varying,
    has_ring boolean DEFAULT false NOT NULL
);


--
-- Name: game_federation_building; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.game_federation_building (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    federation_group_id uuid NOT NULL,
    hex_q integer NOT NULL,
    hex_r integer NOT NULL
);


--
-- Name: game_federation_group; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.game_federation_group (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    game_id uuid NOT NULL,
    player_id uuid NOT NULL,
    federation_tile_code character varying(50) NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    used boolean DEFAULT false NOT NULL
);


--
-- Name: game_federation_offer; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.game_federation_offer (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    game_id uuid NOT NULL,
    federation_tile_type character varying(50) NOT NULL,
    quantity integer NOT NULL,
    "position" integer,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    version bigint DEFAULT 0 NOT NULL
);


--
-- Name: game_federation_token_hex; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.game_federation_token_hex (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    federation_group_id uuid NOT NULL,
    hex_q integer NOT NULL,
    hex_r integer NOT NULL
);


--
-- Name: game_final_scoring; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.game_final_scoring (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    game_id uuid NOT NULL,
    "position" integer NOT NULL,
    scoring_tile_code character varying(50) NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: game_hex; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.game_hex (
    game_id uuid NOT NULL,
    hex_q integer NOT NULL,
    hex_r integer NOT NULL,
    planet_type character varying(30) NOT NULL,
    sector_id character varying(30),
    position_no integer
);


--
-- Name: game_leech_offer; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.game_leech_offer (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    game_id uuid NOT NULL,
    batch_key character varying(36) NOT NULL,
    trigger_player_id uuid NOT NULL,
    receive_player_id uuid NOT NULL,
    receive_seat_no integer NOT NULL,
    power_amount integer NOT NULL,
    vp_cost integer NOT NULL,
    status character varying(20) NOT NULL,
    is_taklons boolean DEFAULT false NOT NULL,
    sequence_no integer NOT NULL,
    taklons_choice character varying(20),
    follow_up_type character varying(50),
    follow_up_data text,
    created_at timestamp without time zone NOT NULL,
    decided_at timestamp without time zone
);


--
-- Name: game_participant; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.game_participant (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    game_id uuid NOT NULL,
    player_id uuid NOT NULL,
    entered_at timestamp without time zone DEFAULT now() NOT NULL,
    claimed_seat_no integer,
    rejoin_token character varying(32) NOT NULL,
    CONSTRAINT chk_claimed_seat_range CHECK (((claimed_seat_no IS NULL) OR ((claimed_seat_no >= 1) AND (claimed_seat_no <= 4))))
);


--
-- Name: game_player_artifact; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.game_player_artifact (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    game_id uuid NOT NULL,
    player_id uuid NOT NULL,
    artifact_type character varying(50) NOT NULL,
    acquired_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: game_player_federation_token; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.game_player_federation_token (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    game_id uuid NOT NULL,
    player_id uuid NOT NULL,
    federation_tile_type character varying(50) NOT NULL,
    acquired_at timestamp without time zone DEFAULT now() NOT NULL,
    used boolean DEFAULT false NOT NULL
);


--
-- Name: game_player_fleet_probe; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.game_player_fleet_probe (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    game_id uuid NOT NULL,
    player_id uuid NOT NULL,
    fleet_name character varying(30) NOT NULL,
    placed_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: game_player_pass; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.game_player_pass (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    game_id uuid NOT NULL,
    player_id uuid NOT NULL,
    round_number integer NOT NULL,
    passed_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: game_player_round_booster; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.game_player_round_booster (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    game_id uuid NOT NULL,
    player_id uuid NOT NULL,
    round_booster_type character varying(50) NOT NULL,
    selected_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: game_player_state; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.game_player_state (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    game_id uuid NOT NULL,
    player_id uuid NOT NULL,
    seat_no integer NOT NULL,
    ore integer DEFAULT 0 NOT NULL,
    credit integer DEFAULT 0 NOT NULL,
    knowledge integer DEFAULT 0 NOT NULL,
    qic integer DEFAULT 0 NOT NULL,
    power_bowl_1 integer DEFAULT 0 NOT NULL,
    power_bowl_2 integer DEFAULT 0 NOT NULL,
    power_bowl_3 integer DEFAULT 0 NOT NULL,
    victory_points integer DEFAULT 0 NOT NULL,
    brainstone_bowl integer,
    tech_terraforming integer DEFAULT 0 NOT NULL,
    tech_navigation integer DEFAULT 0 NOT NULL,
    tech_ai integer DEFAULT 0 NOT NULL,
    tech_gaia integer DEFAULT 0 NOT NULL,
    tech_economy integer DEFAULT 0 NOT NULL,
    tech_science integer DEFAULT 0 NOT NULL,
    stock_mine integer DEFAULT 8 NOT NULL,
    stock_trading_station integer DEFAULT 4 NOT NULL,
    stock_research_lab integer DEFAULT 3 NOT NULL,
    stock_planetary_institute integer DEFAULT 1 NOT NULL,
    stock_academy integer DEFAULT 2 NOT NULL,
    stock_gaiaformer integer DEFAULT 0 NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    faction_type character varying(30),
    booster_action_used boolean DEFAULT false NOT NULL,
    gaia_power integer DEFAULT 0 NOT NULL,
    faction_ability_used boolean DEFAULT false NOT NULL,
    baltaks_converted_gaiaformers integer DEFAULT 0 NOT NULL,
    permanently_removed_gaiaformers integer DEFAULT 0 NOT NULL,
    tinkeroids_used_actions character varying(200) DEFAULT ''::character varying,
    tinkeroids_current_action character varying(50) DEFAULT NULL::character varying,
    qic_academy_action_used boolean DEFAULT false NOT NULL,
    gleens_has_qic_academy boolean DEFAULT false NOT NULL,
    federation_count integer DEFAULT 0 NOT NULL,
    bid_penalty integer DEFAULT 0 NOT NULL,
    used_time_seconds integer DEFAULT 0 NOT NULL,
    turn_started_at timestamp without time zone,
    nickname character varying(50),
    CONSTRAINT chk_brainstone_bowl CHECK (((brainstone_bowl IS NULL) OR ((brainstone_bowl >= 0) AND (brainstone_bowl <= 3))))
);


--
-- Name: game_player_tech_tile; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.game_player_tech_tile (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    game_id uuid NOT NULL,
    player_id uuid NOT NULL,
    tech_tile_code character varying(50) NOT NULL,
    is_covered boolean DEFAULT false NOT NULL,
    covered_by character varying(50),
    acquired_at timestamp without time zone DEFAULT now() NOT NULL,
    covered_at timestamp without time zone,
    action_used boolean DEFAULT false
);


--
-- Name: game_power_action_usage; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.game_power_action_usage (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    game_id uuid NOT NULL,
    round_number integer NOT NULL,
    power_action_type character varying(100) NOT NULL,
    player_id uuid NOT NULL,
    used_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: game_round_scoring; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.game_round_scoring (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    game_id uuid NOT NULL,
    round_number integer NOT NULL,
    scoring_tile_code character varying(50) NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: game_seat; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.game_seat (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    game_id uuid NOT NULL,
    seat_no integer NOT NULL,
    turn_order integer NOT NULL,
    faction_type character varying(50) NOT NULL,
    player_id uuid,
    joined_at timestamp without time zone,
    nickname character varying(50),
    CONSTRAINT chk_seat_no_range CHECK (((seat_no >= 1) AND (seat_no <= 4))),
    CONSTRAINT chk_turn_order_range CHECK (((turn_order >= 0) AND (turn_order <= 4)))
);


--
-- Name: game_sector_placement; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.game_sector_placement (
    game_id uuid NOT NULL,
    position_no integer NOT NULL,
    sector_id character varying(20) NOT NULL,
    rotation integer NOT NULL,
    CONSTRAINT chk_rotation CHECK ((rotation = ANY (ARRAY[0, 60, 120, 180, 240, 300])))
);


--
-- Name: game_single_hex_placement; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.game_single_hex_placement (
    game_id uuid NOT NULL,
    position_no integer NOT NULL,
    tile_type character varying(50) NOT NULL
);


--
-- Name: game_tech_offer; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.game_tech_offer (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    game_id uuid NOT NULL,
    "position" integer NOT NULL,
    tech_track character varying(30) NOT NULL,
    tech_tile_code character varying(50) NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    updated_at timestamp without time zone DEFAULT now() NOT NULL,
    version bigint DEFAULT 0 NOT NULL,
    taken_by_player_id uuid
);


--
-- Name: game_vp_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.game_vp_log (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    game_id uuid NOT NULL,
    player_id uuid NOT NULL,
    category character varying(50) NOT NULL,
    amount integer NOT NULL,
    round_number integer,
    description character varying(200),
    created_at timestamp without time zone DEFAULT now() NOT NULL,
    nickname character varying(50)
);


--
-- Name: player; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.player (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    nickname character varying(100) NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: registered_player; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.registered_player (
    id uuid DEFAULT gen_random_uuid() NOT NULL,
    nickname character varying(100) NOT NULL,
    created_at timestamp without time zone DEFAULT now() NOT NULL
);


--
-- Name: game_action game_action_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_action
    ADD CONSTRAINT game_action_pkey PRIMARY KEY (id);


--
-- Name: game_adv_tech_offer game_adv_tech_offer_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_adv_tech_offer
    ADD CONSTRAINT game_adv_tech_offer_pkey PRIMARY KEY (id);


--
-- Name: game_artifact_offer game_artifact_offer_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_artifact_offer
    ADD CONSTRAINT game_artifact_offer_pkey PRIMARY KEY (id);


--
-- Name: game_bid game_bid_game_id_player_id_key; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_bid
    ADD CONSTRAINT game_bid_game_id_player_id_key UNIQUE (game_id, player_id);


--
-- Name: game_bid game_bid_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_bid
    ADD CONSTRAINT game_bid_pkey PRIMARY KEY (id);


--
-- Name: game_booster_offer game_booster_offer_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_booster_offer
    ADD CONSTRAINT game_booster_offer_pkey PRIMARY KEY (id);


--
-- Name: game_building game_building_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_building
    ADD CONSTRAINT game_building_pkey PRIMARY KEY (id);


--
-- Name: game_federation_building game_federation_building_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_federation_building
    ADD CONSTRAINT game_federation_building_pkey PRIMARY KEY (id);


--
-- Name: game_federation_group game_federation_group_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_federation_group
    ADD CONSTRAINT game_federation_group_pkey PRIMARY KEY (id);


--
-- Name: game_federation_offer game_federation_offer_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_federation_offer
    ADD CONSTRAINT game_federation_offer_pkey PRIMARY KEY (id);


--
-- Name: game_federation_token_hex game_federation_token_hex_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_federation_token_hex
    ADD CONSTRAINT game_federation_token_hex_pkey PRIMARY KEY (id);


--
-- Name: game_final_scoring game_final_scoring_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_final_scoring
    ADD CONSTRAINT game_final_scoring_pkey PRIMARY KEY (id);


--
-- Name: game_hex game_hex_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_hex
    ADD CONSTRAINT game_hex_pkey PRIMARY KEY (game_id, hex_q, hex_r);


--
-- Name: game_participant game_participant_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_participant
    ADD CONSTRAINT game_participant_pkey PRIMARY KEY (id);


--
-- Name: game game_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game
    ADD CONSTRAINT game_pkey PRIMARY KEY (id);


--
-- Name: game_player_artifact game_player_artifact_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_player_artifact
    ADD CONSTRAINT game_player_artifact_pkey PRIMARY KEY (id);


--
-- Name: game_player_federation_token game_player_federation_token_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_player_federation_token
    ADD CONSTRAINT game_player_federation_token_pkey PRIMARY KEY (id);


--
-- Name: game_player_fleet_probe game_player_fleet_probe_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_player_fleet_probe
    ADD CONSTRAINT game_player_fleet_probe_pkey PRIMARY KEY (id);


--
-- Name: game_player_pass game_player_pass_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_player_pass
    ADD CONSTRAINT game_player_pass_pkey PRIMARY KEY (id);


--
-- Name: game_player_round_booster game_player_round_booster_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_player_round_booster
    ADD CONSTRAINT game_player_round_booster_pkey PRIMARY KEY (id);


--
-- Name: game_player_state game_player_state_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_player_state
    ADD CONSTRAINT game_player_state_pkey PRIMARY KEY (id);


--
-- Name: game_player_tech_tile game_player_tech_tile_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_player_tech_tile
    ADD CONSTRAINT game_player_tech_tile_pkey PRIMARY KEY (id);


--
-- Name: game_power_action_usage game_power_action_usage_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_power_action_usage
    ADD CONSTRAINT game_power_action_usage_pkey PRIMARY KEY (id);


--
-- Name: game_round_scoring game_round_scoring_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_round_scoring
    ADD CONSTRAINT game_round_scoring_pkey PRIMARY KEY (id);


--
-- Name: game_seat game_seat_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_seat
    ADD CONSTRAINT game_seat_pkey PRIMARY KEY (id);


--
-- Name: game_sector_placement game_sector_placement_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_sector_placement
    ADD CONSTRAINT game_sector_placement_pkey PRIMARY KEY (game_id, position_no);


--
-- Name: game_single_hex_placement game_single_hex_placement_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_single_hex_placement
    ADD CONSTRAINT game_single_hex_placement_pkey PRIMARY KEY (game_id, position_no);


--
-- Name: game_tech_offer game_tech_offer_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_tech_offer
    ADD CONSTRAINT game_tech_offer_pkey PRIMARY KEY (id);


--
-- Name: game_vp_log game_vp_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_vp_log
    ADD CONSTRAINT game_vp_log_pkey PRIMARY KEY (id);


--
-- Name: game_leech_offer pk_game_leech_offer; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_leech_offer
    ADD CONSTRAINT pk_game_leech_offer PRIMARY KEY (id);


--
-- Name: player player_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.player
    ADD CONSTRAINT player_pkey PRIMARY KEY (id);


--
-- Name: registered_player registered_player_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.registered_player
    ADD CONSTRAINT registered_player_pkey PRIMARY KEY (id);


--
-- Name: game_adv_tech_offer uq_game_adv_offer_code; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_adv_tech_offer
    ADD CONSTRAINT uq_game_adv_offer_code UNIQUE (game_id, adv_tech_tile_code);


--
-- Name: game_adv_tech_offer uq_game_adv_offer_track; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_adv_tech_offer
    ADD CONSTRAINT uq_game_adv_offer_track UNIQUE (game_id, tech_track);


--
-- Name: game_artifact_offer uq_game_artifact_offer; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_artifact_offer
    ADD CONSTRAINT uq_game_artifact_offer UNIQUE (game_id, artifact_type);


--
-- Name: game_artifact_offer uq_game_artifact_position; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_artifact_offer
    ADD CONSTRAINT uq_game_artifact_position UNIQUE (game_id, "position");


--
-- Name: game_booster_offer uq_game_booster_offer_code; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_booster_offer
    ADD CONSTRAINT uq_game_booster_offer_code UNIQUE (game_id, booster_code);


--
-- Name: game_booster_offer uq_game_booster_offer_pos; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_booster_offer
    ADD CONSTRAINT uq_game_booster_offer_pos UNIQUE (game_id, "position");


--
-- Name: game_federation_offer uq_game_federation_offer; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_federation_offer
    ADD CONSTRAINT uq_game_federation_offer UNIQUE (game_id, federation_tile_type, "position");


--
-- Name: game_final_scoring uq_game_final_scoring; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_final_scoring
    ADD CONSTRAINT uq_game_final_scoring UNIQUE (game_id, "position");


--
-- Name: game_participant uq_game_participant; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_participant
    ADD CONSTRAINT uq_game_participant UNIQUE (game_id, player_id);


--
-- Name: game_player_state uq_game_player_state; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_player_state
    ADD CONSTRAINT uq_game_player_state UNIQUE (game_id, player_id);


--
-- Name: game_power_action_usage uq_game_power_action_usage; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_power_action_usage
    ADD CONSTRAINT uq_game_power_action_usage UNIQUE (game_id, round_number, power_action_type);


--
-- Name: game_round_scoring uq_game_round_scoring; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_round_scoring
    ADD CONSTRAINT uq_game_round_scoring UNIQUE (game_id, round_number);


--
-- Name: game_seat uq_game_seat; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_seat
    ADD CONSTRAINT uq_game_seat UNIQUE (game_id, seat_no);


--
-- Name: game_tech_offer uq_game_tech_offer_code; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_tech_offer
    ADD CONSTRAINT uq_game_tech_offer_code UNIQUE (game_id, tech_tile_code);


--
-- Name: game_tech_offer uq_game_tech_offer_pos; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_tech_offer
    ADD CONSTRAINT uq_game_tech_offer_pos UNIQUE (game_id, "position");


--
-- Name: game_player_fleet_probe uq_player_fleet_probe; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_player_fleet_probe
    ADD CONSTRAINT uq_player_fleet_probe UNIQUE (game_id, player_id, fleet_name);


--
-- Name: game_player_pass uq_player_pass_round; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_player_pass
    ADD CONSTRAINT uq_player_pass_round UNIQUE (game_id, player_id, round_number);


--
-- Name: game_player_round_booster uq_player_round_booster; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_player_round_booster
    ADD CONSTRAINT uq_player_round_booster UNIQUE (game_id, player_id);


--
-- Name: game_player_tech_tile uq_player_tech_tile; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_player_tech_tile
    ADD CONSTRAINT uq_player_tech_tile UNIQUE (game_id, player_id, tech_tile_code);


--
-- Name: registered_player uq_registered_player_nickname; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.registered_player
    ADD CONSTRAINT uq_registered_player_nickname UNIQUE (nickname);


--
-- Name: idx_game_action_game_player; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_game_action_game_player ON public.game_action USING btree (game_id, player_id);


--
-- Name: idx_game_action_round; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_game_action_round ON public.game_action USING btree (game_id, round_number);


--
-- Name: idx_player_pass_round; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_player_pass_round ON public.game_player_pass USING btree (game_id, round_number);


--
-- Name: idx_vp_log_game_category; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_vp_log_game_category ON public.game_vp_log USING btree (game_id, category);


--
-- Name: idx_vp_log_game_player; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_vp_log_game_player ON public.game_vp_log USING btree (game_id, player_id);


--
-- Name: ix_federation_building_group; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_federation_building_group ON public.game_federation_building USING btree (federation_group_id);


--
-- Name: ix_federation_group_game; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_federation_group_game ON public.game_federation_group USING btree (game_id);


--
-- Name: ix_federation_group_game_player; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_federation_group_game_player ON public.game_federation_group USING btree (game_id, player_id);


--
-- Name: ix_federation_token_hex_group; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_federation_token_hex_group ON public.game_federation_token_hex USING btree (federation_group_id);


--
-- Name: ix_game_adv_tech_offer_game; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_game_adv_tech_offer_game ON public.game_adv_tech_offer USING btree (game_id);


--
-- Name: ix_game_artifact_offer_game; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_game_artifact_offer_game ON public.game_artifact_offer USING btree (game_id);


--
-- Name: ix_game_booster_offer_game; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_game_booster_offer_game ON public.game_booster_offer USING btree (game_id);


--
-- Name: ix_game_building_game; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_game_building_game ON public.game_building USING btree (game_id);


--
-- Name: ix_game_building_player; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_game_building_player ON public.game_building USING btree (game_id, player_id);


--
-- Name: ix_game_federation_offer_game; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_game_federation_offer_game ON public.game_federation_offer USING btree (game_id);


--
-- Name: ix_game_final_scoring_game; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_game_final_scoring_game ON public.game_final_scoring USING btree (game_id);


--
-- Name: ix_game_hex_game; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_game_hex_game ON public.game_hex USING btree (game_id);


--
-- Name: ix_game_hex_planet; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_game_hex_planet ON public.game_hex USING btree (game_id, planet_type);


--
-- Name: ix_game_power_action_game_round; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_game_power_action_game_round ON public.game_power_action_usage USING btree (game_id, round_number);


--
-- Name: ix_game_power_action_player; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_game_power_action_player ON public.game_power_action_usage USING btree (player_id);


--
-- Name: ix_game_round_scoring_game; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_game_round_scoring_game ON public.game_round_scoring USING btree (game_id);


--
-- Name: ix_game_single_hex_placement_game; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_game_single_hex_placement_game ON public.game_single_hex_placement USING btree (game_id);


--
-- Name: ix_game_tech_offer_game; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_game_tech_offer_game ON public.game_tech_offer USING btree (game_id);


--
-- Name: ix_player_artifact_game_player; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_player_artifact_game_player ON public.game_player_artifact USING btree (game_id, player_id);


--
-- Name: ix_player_artifact_player; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_player_artifact_player ON public.game_player_artifact USING btree (player_id);


--
-- Name: ix_player_federation_token_game_player; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_player_federation_token_game_player ON public.game_player_federation_token USING btree (game_id, player_id);


--
-- Name: ix_player_federation_token_player; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_player_federation_token_player ON public.game_player_federation_token USING btree (player_id);


--
-- Name: ix_player_fleet_probe_game_player; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_player_fleet_probe_game_player ON public.game_player_fleet_probe USING btree (game_id, player_id);


--
-- Name: ix_player_fleet_probe_player; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_player_fleet_probe_player ON public.game_player_fleet_probe USING btree (player_id);


--
-- Name: ix_player_round_booster_player; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_player_round_booster_player ON public.game_player_round_booster USING btree (player_id);


--
-- Name: ix_player_tech_tile_active; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_player_tech_tile_active ON public.game_player_tech_tile USING btree (game_id, player_id, is_covered);


--
-- Name: ix_player_tech_tile_player; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX ix_player_tech_tile_player ON public.game_player_tech_tile USING btree (game_id, player_id);


--
-- Name: uq_game_room_code; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_game_room_code ON public.game USING btree (room_code) WHERE (room_code IS NOT NULL);


--
-- Name: uq_game_seat_player_not_null; Type: INDEX; Schema: public; Owner: -
--

CREATE UNIQUE INDEX uq_game_seat_player_not_null ON public.game_seat USING btree (game_id, player_id) WHERE (player_id IS NOT NULL);


--
-- Name: game_action game_action_game_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_action
    ADD CONSTRAINT game_action_game_id_fkey FOREIGN KEY (game_id) REFERENCES public.game(id) ON DELETE CASCADE;


--
-- Name: game_action game_action_player_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_action
    ADD CONSTRAINT game_action_player_id_fkey FOREIGN KEY (player_id) REFERENCES public.player(id) ON DELETE CASCADE;


--
-- Name: game_adv_tech_offer game_adv_tech_offer_game_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_adv_tech_offer
    ADD CONSTRAINT game_adv_tech_offer_game_id_fkey FOREIGN KEY (game_id) REFERENCES public.game(id) ON DELETE CASCADE;


--
-- Name: game_artifact_offer game_artifact_offer_acquired_by_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_artifact_offer
    ADD CONSTRAINT game_artifact_offer_acquired_by_fkey FOREIGN KEY (acquired_by) REFERENCES public.player(id);


--
-- Name: game_artifact_offer game_artifact_offer_game_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_artifact_offer
    ADD CONSTRAINT game_artifact_offer_game_id_fkey FOREIGN KEY (game_id) REFERENCES public.game(id) ON DELETE CASCADE;


--
-- Name: game_bid game_bid_game_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_bid
    ADD CONSTRAINT game_bid_game_id_fkey FOREIGN KEY (game_id) REFERENCES public.game(id);


--
-- Name: game_booster_offer game_booster_offer_game_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_booster_offer
    ADD CONSTRAINT game_booster_offer_game_id_fkey FOREIGN KEY (game_id) REFERENCES public.game(id) ON DELETE CASCADE;


--
-- Name: game_building game_building_game_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_building
    ADD CONSTRAINT game_building_game_id_fkey FOREIGN KEY (game_id) REFERENCES public.game(id) ON DELETE CASCADE;


--
-- Name: game_building game_building_player_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_building
    ADD CONSTRAINT game_building_player_id_fkey FOREIGN KEY (player_id) REFERENCES public.player(id) ON DELETE CASCADE;


--
-- Name: game_federation_building game_federation_building_federation_group_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_federation_building
    ADD CONSTRAINT game_federation_building_federation_group_id_fkey FOREIGN KEY (federation_group_id) REFERENCES public.game_federation_group(id) ON DELETE CASCADE;


--
-- Name: game_federation_group game_federation_group_game_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_federation_group
    ADD CONSTRAINT game_federation_group_game_id_fkey FOREIGN KEY (game_id) REFERENCES public.game(id) ON DELETE CASCADE;


--
-- Name: game_federation_offer game_federation_offer_game_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_federation_offer
    ADD CONSTRAINT game_federation_offer_game_id_fkey FOREIGN KEY (game_id) REFERENCES public.game(id) ON DELETE CASCADE;


--
-- Name: game_federation_token_hex game_federation_token_hex_federation_group_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_federation_token_hex
    ADD CONSTRAINT game_federation_token_hex_federation_group_id_fkey FOREIGN KEY (federation_group_id) REFERENCES public.game_federation_group(id) ON DELETE CASCADE;


--
-- Name: game_final_scoring game_final_scoring_game_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_final_scoring
    ADD CONSTRAINT game_final_scoring_game_id_fkey FOREIGN KEY (game_id) REFERENCES public.game(id) ON DELETE CASCADE;


--
-- Name: game_hex game_hex_game_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_hex
    ADD CONSTRAINT game_hex_game_id_fkey FOREIGN KEY (game_id) REFERENCES public.game(id) ON DELETE CASCADE;


--
-- Name: game_participant game_participant_game_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_participant
    ADD CONSTRAINT game_participant_game_id_fkey FOREIGN KEY (game_id) REFERENCES public.game(id) ON DELETE CASCADE;


--
-- Name: game_participant game_participant_player_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_participant
    ADD CONSTRAINT game_participant_player_id_fkey FOREIGN KEY (player_id) REFERENCES public.player(id) ON DELETE CASCADE;


--
-- Name: game_player_artifact game_player_artifact_game_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_player_artifact
    ADD CONSTRAINT game_player_artifact_game_id_fkey FOREIGN KEY (game_id) REFERENCES public.game(id) ON DELETE CASCADE;


--
-- Name: game_player_artifact game_player_artifact_player_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_player_artifact
    ADD CONSTRAINT game_player_artifact_player_id_fkey FOREIGN KEY (player_id) REFERENCES public.player(id) ON DELETE CASCADE;


--
-- Name: game_player_federation_token game_player_federation_token_game_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_player_federation_token
    ADD CONSTRAINT game_player_federation_token_game_id_fkey FOREIGN KEY (game_id) REFERENCES public.game(id) ON DELETE CASCADE;


--
-- Name: game_player_federation_token game_player_federation_token_player_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_player_federation_token
    ADD CONSTRAINT game_player_federation_token_player_id_fkey FOREIGN KEY (player_id) REFERENCES public.player(id) ON DELETE CASCADE;


--
-- Name: game_player_fleet_probe game_player_fleet_probe_game_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_player_fleet_probe
    ADD CONSTRAINT game_player_fleet_probe_game_id_fkey FOREIGN KEY (game_id) REFERENCES public.game(id) ON DELETE CASCADE;


--
-- Name: game_player_fleet_probe game_player_fleet_probe_player_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_player_fleet_probe
    ADD CONSTRAINT game_player_fleet_probe_player_id_fkey FOREIGN KEY (player_id) REFERENCES public.player(id) ON DELETE CASCADE;


--
-- Name: game_player_pass game_player_pass_game_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_player_pass
    ADD CONSTRAINT game_player_pass_game_id_fkey FOREIGN KEY (game_id) REFERENCES public.game(id) ON DELETE CASCADE;


--
-- Name: game_player_pass game_player_pass_player_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_player_pass
    ADD CONSTRAINT game_player_pass_player_id_fkey FOREIGN KEY (player_id) REFERENCES public.player(id) ON DELETE CASCADE;


--
-- Name: game_player_round_booster game_player_round_booster_game_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_player_round_booster
    ADD CONSTRAINT game_player_round_booster_game_id_fkey FOREIGN KEY (game_id) REFERENCES public.game(id) ON DELETE CASCADE;


--
-- Name: game_player_round_booster game_player_round_booster_player_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_player_round_booster
    ADD CONSTRAINT game_player_round_booster_player_id_fkey FOREIGN KEY (player_id) REFERENCES public.player(id) ON DELETE CASCADE;


--
-- Name: game_player_state game_player_state_game_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_player_state
    ADD CONSTRAINT game_player_state_game_id_fkey FOREIGN KEY (game_id) REFERENCES public.game(id) ON DELETE CASCADE;


--
-- Name: game_player_state game_player_state_player_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_player_state
    ADD CONSTRAINT game_player_state_player_id_fkey FOREIGN KEY (player_id) REFERENCES public.player(id) ON DELETE CASCADE;


--
-- Name: game_player_tech_tile game_player_tech_tile_game_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_player_tech_tile
    ADD CONSTRAINT game_player_tech_tile_game_id_fkey FOREIGN KEY (game_id) REFERENCES public.game(id) ON DELETE CASCADE;


--
-- Name: game_player_tech_tile game_player_tech_tile_player_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_player_tech_tile
    ADD CONSTRAINT game_player_tech_tile_player_id_fkey FOREIGN KEY (player_id) REFERENCES public.player(id) ON DELETE CASCADE;


--
-- Name: game_power_action_usage game_power_action_usage_game_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_power_action_usage
    ADD CONSTRAINT game_power_action_usage_game_id_fkey FOREIGN KEY (game_id) REFERENCES public.game(id) ON DELETE CASCADE;


--
-- Name: game_power_action_usage game_power_action_usage_player_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_power_action_usage
    ADD CONSTRAINT game_power_action_usage_player_id_fkey FOREIGN KEY (player_id) REFERENCES public.player(id);


--
-- Name: game_round_scoring game_round_scoring_game_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_round_scoring
    ADD CONSTRAINT game_round_scoring_game_id_fkey FOREIGN KEY (game_id) REFERENCES public.game(id) ON DELETE CASCADE;


--
-- Name: game_seat game_seat_game_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_seat
    ADD CONSTRAINT game_seat_game_id_fkey FOREIGN KEY (game_id) REFERENCES public.game(id) ON DELETE CASCADE;


--
-- Name: game_seat game_seat_player_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_seat
    ADD CONSTRAINT game_seat_player_id_fkey FOREIGN KEY (player_id) REFERENCES public.player(id);


--
-- Name: game_sector_placement game_sector_placement_game_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_sector_placement
    ADD CONSTRAINT game_sector_placement_game_id_fkey FOREIGN KEY (game_id) REFERENCES public.game(id) ON DELETE CASCADE;


--
-- Name: game_single_hex_placement game_single_hex_placement_game_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_single_hex_placement
    ADD CONSTRAINT game_single_hex_placement_game_id_fkey FOREIGN KEY (game_id) REFERENCES public.game(id) ON DELETE CASCADE;


--
-- Name: game_tech_offer game_tech_offer_game_id_fkey; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.game_tech_offer
    ADD CONSTRAINT game_tech_offer_game_id_fkey FOREIGN KEY (game_id) REFERENCES public.game(id) ON DELETE CASCADE;


--
--


