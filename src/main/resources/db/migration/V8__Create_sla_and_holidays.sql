CREATE TABLE event_type_sla (
    id                            UUID PRIMARY KEY,
    event_type                    VARCHAR(50) NOT NULL UNIQUE,
    delai_heures_ouvrables        INTEGER NOT NULL,
    delai_avant_evenement_heures  INTEGER NOT NULL
);

INSERT INTO event_type_sla (id, event_type, delai_heures_ouvrables, delai_avant_evenement_heures) VALUES
    (gen_random_uuid(), 'REUNION',    48, 24),
    (gen_random_uuid(), 'CONFERENCE', 48, 24),
    (gen_random_uuid(), 'ATELIER',    48, 24),
    (gen_random_uuid(), 'SEMINAIRE',  48, 24),
    (gen_random_uuid(), 'FORMATION',  48, 24),
    (gen_random_uuid(), 'MISSION',    48, 24),
    (gen_random_uuid(), 'AUDIENCE',   48, 24),
    (gen_random_uuid(), 'AUTRE',      48, 24);

CREATE TABLE jour_ferie (
    id      UUID PRIMARY KEY,
    date    DATE NOT NULL UNIQUE,
    libelle VARCHAR(200)
);

ALTER TABLE event ADD COLUMN soumis_le TIMESTAMP;
ALTER TABLE event ADD COLUMN echeance_validation TIMESTAMP;
