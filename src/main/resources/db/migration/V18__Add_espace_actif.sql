-- Un espace ne doit plus jamais être supprimé physiquement (perte des événements/membres
-- rattachés) : on le désactive à la place, ce qui bloque juste la création de nouveaux
-- événements dedans (voir EspaceServiceImpl.peutCreerDans) sans toucher à l'historique.
ALTER TABLE espace ADD COLUMN actif BOOLEAN NOT NULL DEFAULT TRUE;
