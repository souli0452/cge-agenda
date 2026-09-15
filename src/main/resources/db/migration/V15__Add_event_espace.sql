ALTER TABLE event ADD COLUMN espace_id UUID REFERENCES espace(id);

-- Espace "legacy" par défaut pour les événements créés avant le passage au modèle
-- multi-espaces (aucune règle ne permet de reconstituer rétroactivement "quel chef" pour
-- chacun) : à réaffecter manuellement par la suite si besoin. chef_email neutre pour ne
-- pas entrer en conflit avec un vrai espace créé ensuite pour un chef existant.
INSERT INTO espace (id, nom, chef_email, chef_nom, created_at)
SELECT gen_random_uuid(), 'Espace legacy (à réaffecter)', 'legacy@asce-lc.bf', 'Non affecté', now()
WHERE NOT EXISTS (SELECT 1 FROM espace WHERE chef_email = 'legacy@asce-lc.bf');

UPDATE event SET espace_id = (SELECT id FROM espace WHERE chef_email = 'legacy@asce-lc.bf')
WHERE espace_id IS NULL;
