-- Index sur la table event
CREATE INDEX IF NOT EXISTS idx_event_start_date ON event (start_date);
CREATE INDEX IF NOT EXISTS idx_event_date_range ON event (start_date, end_date);
CREATE INDEX IF NOT EXISTS idx_event_status ON event (status);
CREATE INDEX IF NOT EXISTS idx_event_type ON event (type);
CREATE INDEX IF NOT EXISTS idx_event_status_type_date ON event (status, type, start_date);
CREATE INDEX IF NOT EXISTS idx_event_year ON event ((EXTRACT(YEAR FROM start_date)));
CREATE INDEX IF NOT EXISTS idx_event_year_month_status ON event (
    EXTRACT(YEAR FROM start_date),
    EXTRACT(MONTH FROM start_date),
    status
);

-- Index sur la table participant_event
CREATE INDEX IF NOT EXISTS idx_participant_event_event_id ON participant_event (event_id);
CREATE INDEX IF NOT EXISTS idx_participant_event_participant_id ON participant_event (participant_id);
CREATE INDEX IF NOT EXISTS idx_participant_event_event_participant ON participant_event (event_id, participant_id);