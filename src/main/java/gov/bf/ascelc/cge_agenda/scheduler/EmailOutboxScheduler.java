package gov.bf.ascelc.cge_agenda.scheduler;

import gov.bf.ascelc.cge_agenda.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Retente périodiquement l'envoi des emails en attente dans l'outbox (voir
 * {@link gov.bf.ascelc.cge_agenda.entities.EmailOutbox}). Une fréquence d'une minute
 * permet de rattraper rapidement une coupure SMTP de quelques secondes ; le calendrier
 * de backoff propre à chaque email gère lui-même l'espacement des tentatives
 * suivantes (jusqu'à 24h) pour les pannes plus longues.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailOutboxScheduler {

    private final EmailService emailService;

    @Scheduled(fixedDelay = 60_000)
    public void retryPendingEmails() {
        try {
            emailService.processOutbox();
        } catch (Exception e) {
            log.error("❌ Erreur lors du traitement de l'outbox email : {}", e.getMessage());
        }
    }
}
