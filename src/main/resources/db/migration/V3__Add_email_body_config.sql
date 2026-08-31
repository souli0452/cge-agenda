-- Renomme le champ mort "validated" (aucun template/méthode d'envoi ne correspond) en
-- "new_document" (qui existe réellement : new-document.html + sendNewDocumentNotification()
-- mais n'avait jamais eu de champ de configuration).
ALTER TABLE org_config RENAME COLUMN subject_validated TO subject_new_document;

-- Le rename ci-dessus conserve la valeur existante. Pour les installations déjà en place,
-- cette colonne contient encore l'ancien libellé "Événement validé : {evenement}" (utilisé
-- avant l'introduction de ce champ). On ne réinitialise que si la valeur est exactement
-- l'ancien défaut, pour ne pas écraser une éventuelle personnalisation faite entre-temps.
UPDATE org_config SET subject_new_document = 'Nouveau document : {evenement}'
WHERE subject_new_document = 'Événement validé : {evenement}';

-- Corps du message principal, un champ par type d'email (miroir des subject_* existants).
ALTER TABLE org_config ADD COLUMN body_invitation TEXT;
ALTER TABLE org_config ADD COLUMN body_validation_request TEXT;
ALTER TABLE org_config ADD COLUMN body_new_document TEXT;
ALTER TABLE org_config ADD COLUMN body_rejected TEXT;
ALTER TABLE org_config ADD COLUMN body_changes_requested TEXT;
ALTER TABLE org_config ADD COLUMN body_amendments_corrected TEXT;
ALTER TABLE org_config ADD COLUMN body_cancellation TEXT;
ALTER TABLE org_config ADD COLUMN body_postponement TEXT;
ALTER TABLE org_config ADD COLUMN body_event_update TEXT;
ALTER TABLE org_config ADD COLUMN body_reminder TEXT;
ALTER TABLE org_config ADD COLUMN body_delegation TEXT;
