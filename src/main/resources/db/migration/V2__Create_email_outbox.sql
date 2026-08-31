-- File d'attente persistante pour les emails : survit à une panne SMTP de quelques
-- secondes comme de plusieurs jours (voir EmailServiceImpl / EmailOutboxScheduler).
CREATE TABLE IF NOT EXISTS email_outbox (
    id               UUID PRIMARY KEY,
    recipient_email  VARCHAR(255) NOT NULL,
    subject          VARCHAR(500) NOT NULL,
    html_content     TEXT NOT NULL,
    status           VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    attempts         INTEGER NOT NULL DEFAULT 0,
    created_at       TIMESTAMP NOT NULL,
    last_attempt_at  TIMESTAMP,
    next_attempt_at  TIMESTAMP NOT NULL,
    last_error       TEXT,
    context          VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_email_outbox_status_next_attempt
    ON email_outbox (status, next_attempt_at);
