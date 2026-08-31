CREATE TABLE notification (
    id                 UUID PRIMARY KEY,
    destinataire_email VARCHAR(255) NOT NULL,
    type               VARCHAR(50) NOT NULL,
    event_id           UUID,
    message            VARCHAR(500),
    lue                BOOLEAN NOT NULL DEFAULT FALSE,
    created_at         TIMESTAMP NOT NULL
);

CREATE INDEX idx_notification_destinataire ON notification (destinataire_email, lue);
