package gov.bf.ascelc.cge_agenda.service.impl;

import gov.bf.ascelc.cge_agenda.entities.EmailOutbox;
import gov.bf.ascelc.cge_agenda.enums.EmailOutboxStatus;
import gov.bf.ascelc.cge_agenda.repository.EmailOutboxRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Écritures de l'outbox email dans leur PROPRE transaction ({@code REQUIRES_NEW}).
 * Nécessaire car les méthodes d'envoi d'{@link EmailServiceImpl} sont annotées
 * {@code @Transactional(readOnly = true)} : sans transaction dédiée, l'enregistrement
 * de l'email en attente pourrait ne jamais être flushé/committé, et l'email serait
 * perdu silencieusement en cas de panne SMTP au lieu d'être retenté plus tard.
 */
@Service
@RequiredArgsConstructor
public class EmailOutboxService {

    private final EmailOutboxRepository emailOutboxRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public EmailOutbox create(String to, String subject, String htmlContent, String context) {
        LocalDateTime now = LocalDateTime.now();
        EmailOutbox item = EmailOutbox.builder()
                .recipientEmail(to)
                .subject(subject)
                .htmlContent(htmlContent)
                .status(EmailOutboxStatus.PENDING)
                .attempts(0)
                .createdAt(now)
                .nextAttemptAt(now)
                .context(context)
                .build();
        return emailOutboxRepository.save(item);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSent(EmailOutbox item) {
        item.setStatus(EmailOutboxStatus.SENT);
        item.setLastAttemptAt(LocalDateTime.now());
        emailOutboxRepository.save(item);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailedAttempt(EmailOutbox item, int attempts, LocalDateTime nextAttemptAt,
                                   EmailOutboxStatus status, String lastError) {
        item.setAttempts(attempts);
        item.setLastAttemptAt(LocalDateTime.now());
        item.setLastError(lastError);
        item.setStatus(status);
        item.setNextAttemptAt(nextAttemptAt);
        emailOutboxRepository.save(item);
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRES_NEW)
    public List<EmailOutbox> findDue() {
        return emailOutboxRepository.findByStatusAndNextAttemptAtLessThanEqual(
                EmailOutboxStatus.PENDING, LocalDateTime.now());
    }
}
