CREATE TABLE espace (
    id         UUID PRIMARY KEY,
    nom        VARCHAR(200) NOT NULL,
    chef_email VARCHAR(255) NOT NULL UNIQUE,
    chef_nom   VARCHAR(200),
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE membre_espace (
    id               UUID PRIMARY KEY,
    espace_id        UUID NOT NULL REFERENCES espace(id),
    membre_email     VARCHAR(255) NOT NULL,
    membre_nom       VARCHAR(200),
    role             VARCHAR(20) NOT NULL,
    statut           VARCHAR(20) NOT NULL DEFAULT 'INVITE',
    invited_at       TIMESTAMP NOT NULL,
    activated_at     TIMESTAMP,
    invited_by_email VARCHAR(255),
    CONSTRAINT uk_membre_espace UNIQUE (espace_id, membre_email)
);

CREATE INDEX idx_membre_espace_membre ON membre_espace (membre_email, statut);
