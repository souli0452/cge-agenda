-- Schema de base genere depuis la base de developpement (pg_dump --schema-only),
-- pour amorcer une base de production vierge.

--
-- PostgreSQL database dump
--


-- Dumped from database version 18.0
-- Dumped by pg_dump version 18.0

SET statement_timeout = 0;
SET lock_timeout = 0;
SET idle_in_transaction_session_timeout = 0;
SET transaction_timeout = 0;
SET client_encoding = 'UTF8';
SET standard_conforming_strings = on;
SELECT pg_catalog.set_config('search_path', '', false);
SET check_function_bodies = false;
SET xmloption = content;
SET client_min_messages = warning;
SET row_security = off;

SET default_tablespace = '';

SET default_table_access_method = heap;

--
-- Name: audit_log; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.audit_log (
    id uuid NOT NULL,
    action character varying(64) NOT NULL,
    details character varying(2000),
    entity_id character varying(64),
    entity_title character varying(255),
    entity_type character varying(64),
    ip_address character varying(64),
    "timestamp" timestamp(6) without time zone NOT NULL,
    user_agent character varying(512),
    user_email character varying(255),
    user_full_name character varying(255),
    user_id character varying(128),
    user_role character varying(64)
);


--
-- Name: backup_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.backup_config (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_first_name character varying(255),
    current_user_last_name character varying(255),
    current_user_email character varying(255),
    updated_at timestamp(6) without time zone,
    updated_by_id character varying(255),
    auto_enabled boolean NOT NULL,
    backup_hour integer NOT NULL,
    backup_minute integer NOT NULL,
    retention_count integer NOT NULL
);


--
-- Name: event; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.event (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_first_name character varying(255),
    current_user_last_name character varying(255),
    current_user_email character varying(255),
    updated_at timestamp(6) without time zone,
    updated_by_id character varying(255),
    description text,
    end_date date NOT NULL,
    meeting_link character varying(500),
    pays character varying(255),
    start_date date NOT NULL,
    status character varying(50) NOT NULL,
    title character varying(255) NOT NULL,
    type character varying(50) NOT NULL,
    ville character varying(255),
    change_suggestions text,
    creator_email character varying(255),
    creator_role character varying(255),
    creator_username character varying(255),
    delegue_email character varying(255),
    delegue_motif text,
    delegue_nom character varying(255),
    lieu_type character varying(30),
    nom_lieu character varying(200),
    rejection_reason text,
    salle character varying(150),
    validation_comment text,
    deleted boolean DEFAULT false NOT NULL,
    delegue_date timestamp(6) without time zone,
    delegue_par_email character varying(255),
    est_delegue boolean DEFAULT false NOT NULL,
    compte_rendu_actions text,
    compte_rendu_date timestamp(6) without time zone,
    compte_rendu_decisions text,
    compte_rendu_points text,
    compte_rendu_redige_par character varying(255),
    CONSTRAINT event_lieu_type_check CHECK (((lieu_type)::text = ANY (ARRAY[('INTERNE'::character varying)::text, ('NATIONAL'::character varying)::text, ('INTERNATIONAL'::character varying)::text, ('VIRTUEL'::character varying)::text]))),
    CONSTRAINT event_status_check CHECK (((status)::text = ANY (ARRAY[('BROUILLON'::character varying)::text, ('EN_ATTENTE_VALIDATION'::character varying)::text, ('A_CORRIGER'::character varying)::text, ('PLANIFIE'::character varying)::text, ('EN_COURS'::character varying)::text, ('TERMINE'::character varying)::text, ('ANNULER'::character varying)::text, ('REPORTER'::character varying)::text, ('REJETE'::character varying)::text]))),
    CONSTRAINT event_type_check CHECK (((type)::text = ANY (ARRAY[('REUNION'::character varying)::text, ('CONFERENCE'::character varying)::text, ('ATELIER'::character varying)::text, ('SEMINAIRE'::character varying)::text, ('FORMATION'::character varying)::text, ('MISSION'::character varying)::text, ('AUTRE'::character varying)::text])))
);


--
-- Name: file; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.file (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_first_name character varying(255),
    current_user_last_name character varying(255),
    current_user_email character varying(255),
    updated_at timestamp(6) without time zone,
    updated_by_id character varying(255),
    description character varying(255),
    file_name character varying(255) NOT NULL,
    file_path character varying(255) NOT NULL,
    file_size bigint,
    file_type character varying(255),
    event_id uuid NOT NULL
);


--
-- Name: org_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.org_config (
    id uuid NOT NULL,
    adresse character varying(255),
    couleur_primaire character varying(255),
    created_at timestamp(6) without time zone NOT NULL,
    email_expediteur_nom character varying(255),
    logo_url character varying(255),
    nom_organisation character varying(255) NOT NULL,
    site_web character varying(255),
    slogan character varying(255),
    subject_amendments_corrected character varying(255),
    subject_cancellation character varying(255),
    subject_changes_requested character varying(255),
    subject_delegation character varying(255),
    subject_event_update character varying(255),
    subject_invitation character varying(255),
    subject_postponement character varying(255),
    subject_rejected character varying(255),
    subject_reminder character varying(255),
    subject_validated character varying(255),
    subject_validation_request character varying(255),
    updated_at timestamp(6) without time zone NOT NULL,
    created_by_id character varying(255),
    current_user_first_name character varying(255),
    current_user_last_name character varying(255),
    current_user_email character varying(255),
    updated_by_id character varying(255)
);


--
-- Name: participant; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.participant (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_first_name character varying(255),
    current_user_last_name character varying(255),
    current_user_email character varying(255),
    updated_at timestamp(6) without time zone,
    updated_by_id character varying(255),
    email character varying(255) NOT NULL,
    first_name character varying(100) NOT NULL,
    job_title character varying(255),
    last_name character varying(100) NOT NULL,
    structure character varying(255),
    participant_type character varying(50) NOT NULL,
    phone_number character varying(20),
    deleted_at timestamp(6) without time zone,
    deleted_by character varying(255),
    deleted boolean DEFAULT false NOT NULL,
    CONSTRAINT participant_participant_type_check CHECK (((participant_type)::text = ANY (ARRAY[('INTERNE'::character varying)::text, ('EXTERNE'::character varying)::text])))
);


--
-- Name: participant_event; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.participant_event (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_first_name character varying(255),
    current_user_last_name character varying(255),
    current_user_email character varying(255),
    updated_at timestamp(6) without time zone,
    updated_by_id character varying(255),
    event_id uuid NOT NULL,
    participant_id uuid NOT NULL
);


--
-- Name: schedule; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.schedule (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_first_name character varying(255),
    current_user_last_name character varying(255),
    current_user_email character varying(255),
    updated_at timestamp(6) without time zone,
    updated_by_id character varying(255),
    address character varying(500),
    date_jour date NOT NULL,
    end_time time(6) without time zone NOT NULL,
    start_time time(6) without time zone NOT NULL,
    event_id uuid NOT NULL
);


--
-- Name: scheduler_config; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.scheduler_config (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone,
    created_by_id character varying(255),
    current_user_first_name character varying(255),
    current_user_last_name character varying(255),
    current_user_email character varying(255),
    updated_at timestamp(6) without time zone,
    updated_by_id character varying(255),
    reminder_enabled boolean NOT NULL,
    send_hour integer NOT NULL
);


--
-- Name: scheduler_config_reminder_days; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.scheduler_config_reminder_days (
    scheduler_config_id uuid NOT NULL,
    days_until integer
);


--
-- Name: user_settings; Type: TABLE; Schema: public; Owner: -
--

CREATE TABLE public.user_settings (
    id uuid NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    email_invitation_enabled boolean NOT NULL,
    email_reminder_enabled boolean NOT NULL,
    email_validation_enabled boolean NOT NULL,
    updated_at timestamp(6) without time zone NOT NULL,
    user_email character varying(255),
    user_id character varying(255) NOT NULL,
    created_by_id character varying(255),
    current_user_first_name character varying(255),
    current_user_last_name character varying(255),
    current_user_email character varying(255),
    updated_by_id character varying(255)
);


--
-- Name: audit_log audit_log_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.audit_log
    ADD CONSTRAINT audit_log_pkey PRIMARY KEY (id);


--
-- Name: backup_config backup_config_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.backup_config
    ADD CONSTRAINT backup_config_pkey PRIMARY KEY (id);


--
-- Name: event event_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.event
    ADD CONSTRAINT event_pkey PRIMARY KEY (id);


--
-- Name: file file_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.file
    ADD CONSTRAINT file_pkey PRIMARY KEY (id);


--
-- Name: org_config org_config_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.org_config
    ADD CONSTRAINT org_config_pkey PRIMARY KEY (id);


--
-- Name: participant_event participant_event_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.participant_event
    ADD CONSTRAINT participant_event_pkey PRIMARY KEY (id);


--
-- Name: participant participant_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.participant
    ADD CONSTRAINT participant_pkey PRIMARY KEY (id);


--
-- Name: schedule schedule_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.schedule
    ADD CONSTRAINT schedule_pkey PRIMARY KEY (id);


--
-- Name: scheduler_config scheduler_config_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scheduler_config
    ADD CONSTRAINT scheduler_config_pkey PRIMARY KEY (id);


--
-- Name: user_settings uk4bos7satl9xeqd18frfeqg6tt; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_settings
    ADD CONSTRAINT uk4bos7satl9xeqd18frfeqg6tt UNIQUE (user_id);


--
-- Name: participant_event uk_participant_event; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.participant_event
    ADD CONSTRAINT uk_participant_event UNIQUE (participant_id, event_id);


--
-- Name: user_settings user_settings_pkey; Type: CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.user_settings
    ADD CONSTRAINT user_settings_pkey PRIMARY KEY (id);


--
-- Name: idx_audit_action; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_action ON public.audit_log USING btree (action);


--
-- Name: idx_audit_entity_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_entity_type ON public.audit_log USING btree (entity_type);


--
-- Name: idx_audit_timestamp; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_timestamp ON public.audit_log USING btree ("timestamp");


--
-- Name: idx_audit_user_email; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_audit_user_email ON public.audit_log USING btree (user_email);


--
-- Name: idx_event_date_range; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_event_date_range ON public.event USING btree (start_date, end_date);


--
-- Name: idx_event_start_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_event_start_date ON public.event USING btree (start_date);


--
-- Name: idx_event_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_event_status ON public.event USING btree (status);


--
-- Name: idx_event_status_type_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_event_status_type_date ON public.event USING btree (status, type, start_date);


--
-- Name: idx_event_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_event_type ON public.event USING btree (type);


--
-- Name: idx_event_year; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_event_year ON public.event USING btree (EXTRACT(year FROM start_date));


--
-- Name: idx_event_year_month_status; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_event_year_month_status ON public.event USING btree (EXTRACT(year FROM start_date), EXTRACT(month FROM start_date), status);


--
-- Name: idx_participant_email; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_participant_email ON public.participant USING btree (email);


--
-- Name: idx_participant_event_event_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_participant_event_event_id ON public.participant_event USING btree (event_id);


--
-- Name: idx_participant_event_event_participant; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_participant_event_event_participant ON public.participant_event USING btree (event_id, participant_id);


--
-- Name: idx_participant_event_participant_id; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_participant_event_participant_id ON public.participant_event USING btree (participant_id);


--
-- Name: idx_participant_structure; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_participant_structure ON public.participant USING btree (structure);


--
-- Name: idx_participant_type; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_participant_type ON public.participant USING btree (participant_type);


--
-- Name: idx_schedule_date; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_schedule_date ON public.schedule USING btree (date_jour);


--
-- Name: idx_schedule_event; Type: INDEX; Schema: public; Owner: -
--

CREATE INDEX idx_schedule_event ON public.schedule USING btree (event_id);


--
-- Name: file fk_file_event; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.file
    ADD CONSTRAINT fk_file_event FOREIGN KEY (event_id) REFERENCES public.event(id);


--
-- Name: participant_event fk_participant_event_event; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.participant_event
    ADD CONSTRAINT fk_participant_event_event FOREIGN KEY (event_id) REFERENCES public.event(id);


--
-- Name: participant_event fk_participant_event_participant; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.participant_event
    ADD CONSTRAINT fk_participant_event_participant FOREIGN KEY (participant_id) REFERENCES public.participant(id);


--
-- Name: schedule fk_schedule_event; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.schedule
    ADD CONSTRAINT fk_schedule_event FOREIGN KEY (event_id) REFERENCES public.event(id);


--
-- Name: scheduler_config_reminder_days fkca7l7ukc41l3tgpxj6n9ilmhg; Type: FK CONSTRAINT; Schema: public; Owner: -
--

ALTER TABLE ONLY public.scheduler_config_reminder_days
    ADD CONSTRAINT fkca7l7ukc41l3tgpxj6n9ilmhg FOREIGN KEY (scheduler_config_id) REFERENCES public.scheduler_config(id);


--
-- PostgreSQL database dump complete
--


