-- Verrou optimiste : évite qu'une validation/rejet concurrent écrase silencieusement
-- une autre modification (deux validateurs ouvrant le même événement en même temps).
ALTER TABLE event ADD COLUMN version INTEGER NOT NULL DEFAULT 0;

-- Traçabilité de la duplication d'un événement rejeté en nouveau brouillon.
ALTER TABLE event ADD COLUMN dupliquee_de_id UUID REFERENCES event(id);
