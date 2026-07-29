-- Renomme le champ mort "validated" (aucun template/méthode d'envoi ne correspond) en
-- "new_document" (qui existe réellement : new-document.html + sendNewDocumentNotification()
-- mais n'avait jamais eu de champ de configuration).
ALTER TABLE org_config RENAME COLUMN subject_validated TO subject_new_document;

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
