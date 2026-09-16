package gov.bf.ascelc.cge_agenda.entities;

import gov.bf.ascelc.cge_agenda.enums.EmailOutboxStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * File d'attente persistante pour les emails : chaque email est enregistré ici avant
 * la tentative d'envoi, afin de survivre à une panne SMTP de quelques secondes comme
 * de plusieurs jours (le job planifié {@code EmailOutboxScheduler} retente les envois
 * en attente selon un calendrier de backoff croissant).
 */
@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "email_outbox")
public class EmailOutbox {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(nullable = false, length = 500)
    private String subject;

    @Lob
    @Column(name = "html_content", nullable = false, columnDefinition = "TEXT")
    private String htmlContent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EmailOutboxStatus status = EmailOutboxStatus.PENDING;

    @Column(nullable = false)
    @Builder.Default
    private int attempts = 0;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Lob
    @Column(name = "last_error", columnDefinition = "TEXT")
    private String lastError;

    /**
     * Contexte lisible pour le diagnostic admin (ex: "INVITATION eventId=... participantId=...").
     */
    @Column(name = "context", length = 500)
    private String context;
}
