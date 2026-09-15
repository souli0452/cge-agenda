-- V4 a créé la colonne "version" en INTEGER, mais l'entité JPA Event la déclare en Long
-- (@Version private Long version), attendu en BIGINT côté base. Avec ddl-auto=validate,
-- ce désaccord de type fait échouer le démarrage de l'application sur toute base créée
-- fraîchement depuis les migrations. Idempotent : ALTER ... TYPE BIGINT ne fait rien si
-- la colonne est déjà bigint.
ALTER TABLE event ALTER COLUMN version TYPE BIGINT;
