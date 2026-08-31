-- Le protocole est désormais scopé par espace (membre_espace, role=PROTOCOLE), plus une
-- adresse globale unique incohérente avec le cloisonnement multi-espaces.
ALTER TABLE org_config DROP COLUMN IF EXISTS protocole_email;
