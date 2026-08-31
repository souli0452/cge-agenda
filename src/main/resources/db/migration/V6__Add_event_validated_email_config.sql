-- Diffusions supplémentaires à la validation d'un événement : le créateur (copie dédiée)
-- et le protocole (nouvelle cellule destinataire, adresse configurable).
ALTER TABLE org_config ADD COLUMN subject_event_validated_creator VARCHAR(255);
ALTER TABLE org_config ADD COLUMN subject_event_validated_protocole VARCHAR(255);
ALTER TABLE org_config ADD COLUMN body_event_validated_creator TEXT;
ALTER TABLE org_config ADD COLUMN body_event_validated_protocole TEXT;
ALTER TABLE org_config ADD COLUMN protocole_email VARCHAR(255);

UPDATE org_config SET
    subject_event_validated_creator = 'Votre événement a été validé : {evenement}',
    subject_event_validated_protocole = 'Événement validé (protocole) : {evenement}',
    body_event_validated_creator = 'Votre événement {evenement} a été validé. Les invitations ont été envoyées aux participants.',
    body_event_validated_protocole = 'Un événement vient d''être validé et nécessite une prise en compte du protocole.'
WHERE subject_event_validated_creator IS NULL;
