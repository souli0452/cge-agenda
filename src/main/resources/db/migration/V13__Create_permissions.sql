CREATE TABLE permission (
    id          UUID PRIMARY KEY,
    cle         VARCHAR(60) NOT NULL UNIQUE,
    description VARCHAR(255)
);

CREATE TABLE role_permission (
    id            UUID PRIMARY KEY,
    role_name     VARCHAR(60) NOT NULL,
    permission_id UUID NOT NULL REFERENCES permission(id),
    CONSTRAINT uk_role_permission UNIQUE (role_name, permission_id)
);

INSERT INTO permission (id, cle, description) VALUES
    (gen_random_uuid(), 'EVENT_VIEW', 'Voir les événements'),
    (gen_random_uuid(), 'EVENT_CREATE', 'Créer un événement'),
    (gen_random_uuid(), 'EVENT_EDIT', 'Modifier un événement'),
    (gen_random_uuid(), 'EVENT_DELETE', 'Supprimer définitivement un événement'),
    (gen_random_uuid(), 'EVENT_CANCEL_POSTPONE', 'Annuler / reporter un événement'),
    (gen_random_uuid(), 'EVENT_VALIDATE', 'Valider un événement'),
    (gen_random_uuid(), 'EVENT_REJECT', 'Rejeter un événement'),
    (gen_random_uuid(), 'EVENT_REQUEST_CHANGES', 'Demander des modifications'),
    (gen_random_uuid(), 'EVENT_DELEGATE', 'Déléguer la participation'),
    (gen_random_uuid(), 'EVENT_OBSERVATION', 'Ajouter une observation'),
    (gen_random_uuid(), 'EVENT_DEMANDER_DELEGATION', 'Demander une délégation'),
    (gen_random_uuid(), 'EVENT_EXPORT_PDF', 'Exporter en PDF'),
    (gen_random_uuid(), 'STATS_VIEW', 'Voir les statistiques'),
    (gen_random_uuid(), 'AUDIT_VIEW', 'Voir le journal d''audit'),
    (gen_random_uuid(), 'ADMIN_CONFIG', 'Gérer la configuration système'),
    (gen_random_uuid(), 'ADMIN_USERS', 'Gérer les comptes utilisateurs'),
    (gen_random_uuid(), 'ESPACE_MANAGE', 'Créer/gérer les espaces');

-- Seed reproduisant EXACTEMENT les droits actuels par rôle (SecurityConfig.java +
-- auth.service.ts avant ce changement), pour ne rien casser au déploiement. L'admin
-- ajuste ensuite depuis l'écran "Rôles & permissions" sans redéploiement.

-- ADMIN : tout.
INSERT INTO role_permission (id, role_name, permission_id)
SELECT gen_random_uuid(), 'ADMIN', id FROM permission;

-- CGE : tout sauf suppression définitive et administration système/comptes/espaces.
INSERT INTO role_permission (id, role_name, permission_id)
SELECT gen_random_uuid(), 'CGE', id FROM permission
WHERE cle IN ('EVENT_VIEW','EVENT_CREATE','EVENT_EDIT','EVENT_CANCEL_POSTPONE',
              'EVENT_VALIDATE','EVENT_REJECT','EVENT_REQUEST_CHANGES','EVENT_DELEGATE',
              'EVENT_OBSERVATION','EVENT_DEMANDER_DELEGATION','EVENT_EXPORT_PDF','STATS_VIEW');

-- DIRECTEUR_CABINET : consultation/écriture de base + stats + export, pas de validation.
INSERT INTO role_permission (id, role_name, permission_id)
SELECT gen_random_uuid(), 'DIRECTEUR_CABINET', id FROM permission
WHERE cle IN ('EVENT_VIEW','EVENT_CREATE','EVENT_EDIT','EVENT_CANCEL_POSTPONE',
              'EVENT_EXPORT_PDF','STATS_VIEW');

-- SECRETAIRE : consultation/écriture de base uniquement.
INSERT INTO role_permission (id, role_name, permission_id)
SELECT gen_random_uuid(), 'SECRETAIRE', id FROM permission
WHERE cle IN ('EVENT_VIEW','EVENT_CREATE','EVENT_EDIT');

-- PROTOCOLE : consultation/écriture de base uniquement.
INSERT INTO role_permission (id, role_name, permission_id)
SELECT gen_random_uuid(), 'PROTOCOLE', id FROM permission
WHERE cle IN ('EVENT_VIEW','EVENT_CREATE','EVENT_EDIT');
