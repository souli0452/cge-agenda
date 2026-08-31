ALTER TABLE org_config ADD COLUMN heure_debut_ouvrable TIME;
ALTER TABLE org_config ADD COLUMN heure_fin_ouvrable TIME;

UPDATE org_config SET
    heure_debut_ouvrable = '07:30:00',
    heure_fin_ouvrable = '17:00:00'
WHERE heure_debut_ouvrable IS NULL;
