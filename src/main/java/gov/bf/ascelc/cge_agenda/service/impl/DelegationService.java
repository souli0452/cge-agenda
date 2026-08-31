package gov.bf.ascelc.cge_agenda.service.impl;

import gov.bf.ascelc.cge_agenda.entities.Event;
import gov.bf.ascelc.cge_agenda.enums.MembreEspaceRole;
import gov.bf.ascelc.cge_agenda.enums.MembreEspaceStatut;
import gov.bf.ascelc.cge_agenda.enums.NotificationType;
import gov.bf.ascelc.cge_agenda.repository.EventRepository;
import gov.bf.ascelc.cge_agenda.repository.MembreEspaceRepository;
import gov.bf.ascelc.cge_agenda.service.AuditService;
import gov.bf.ascelc.cge_agenda.service.EmailService;
import gov.bf.ascelc.cge_agenda.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;

/**
 * Traite les clics sur les liens signés d'accepter/décliner une délégation, envoyés par
 * email et cliqués sans authentification. Volontairement séparé d'EventServiceImpl :
 * ce flux ne passe pas par le contexte de sécurité (pas d'utilisateur connecté).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DelegationService {

    public enum Resultat { ACCEPTEE, DECLINEE, DEJA_TRAITEE }

    private final EventRepository eventRepository;
    private final SignedTokenService signedTokenService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final EmailService emailService;
    private final MembreEspaceRepository membreEspaceRepository;

    @Transactional(rollbackFor = Exception.class)
    public Resultat accepter(String token) {
        Event event = resoudreEvent(token);

        if (event.getDelegationConfirmee() != null) {
            return Resultat.DEJA_TRAITEE;
        }

        event.setDelegationConfirmee(true);
        event.setUpdatedAt(LocalDateTime.now());
        eventRepository.save(event);

        auditService.logAction("DELEGATION_ACCEPTEE", "EVENT", event.getId().toString(), event.getTitle(),
                "acceptée par " + event.getDelegueEmail(), null);
        log.info("✓ Délégation acceptée : {} ({})", event.getId(), event.getDelegueEmail());
        return Resultat.ACCEPTEE;
    }

    @Transactional(rollbackFor = Exception.class)
    public Resultat decliner(String token) {
        Event event = resoudreEvent(token);

        if (event.getDelegationConfirmee() != null) {
            return Resultat.DEJA_TRAITEE;
        }

        event.setDelegationConfirmee(false);
        event.setEstDelegue(false);
        event.setUpdatedAt(LocalDateTime.now());
        Event saved = eventRepository.save(event);

        auditService.logAction("DELEGATION_REFUSEE", "EVENT", saved.getId().toString(), saved.getTitle(),
                "déclinée par " + saved.getDelegueEmail(), null);

        String message = "Le délégué a décliné la délégation pour l'événement \"" + saved.getTitle() + "\".";
        notificationService.notifier(saved.getCreatorEmail(), NotificationType.DELEGATION_REFUSEE, saved.getId(), message);
        if (saved.getEspace() != null) {
            membreEspaceRepository.findByEspaceIdAndRoleAndStatut(
                            saved.getEspace().getId(), MembreEspaceRole.PROTOCOLE, MembreEspaceStatut.ACTIF)
                    .forEach(m -> notificationService.notifier(m.getMembreEmail(), NotificationType.DELEGATION_REFUSEE, saved.getId(), message));
        }
        emailService.sendDelegationDeclined(saved.getId());

        log.info("✓ Délégation déclinée : {} ({})", saved.getId(), saved.getDelegueEmail());
        return Resultat.DECLINEE;
    }

    private Event resoudreEvent(String token) {
        SignedTokenService.Payload payload = signedTokenService.verify(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Lien invalide ou expiré"));

        Event event = eventRepository.findById(payload.eventId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Événement non trouvé"));

        if (!event.isEstDelegue() || event.getDelegueEmail() == null
                || !event.getDelegueEmail().equalsIgnoreCase(payload.email())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cette délégation n'est plus valide");
        }
        return event;
    }
}
