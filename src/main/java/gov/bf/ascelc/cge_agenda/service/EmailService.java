package gov.bf.ascelc.cge_agenda.service;

import gov.bf.ascelc.cge_agenda.entities.Event;
import gov.bf.ascelc.cge_agenda.entities.File;
import gov.bf.ascelc.cge_agenda.entities.Participant;

import java.util.UUID;

public interface EmailService {

    void sendEventReminder(UUID eventId, int daysUntil);

    void sendEventUpdateNotification(UUID eventId);

    void sendNewDocumentNotification(UUID eventId, UUID fileId);

    void sendEventInvitation(UUID eventId, UUID participantId);

    void sendValidationRequest(UUID eventId);

    void sendEventRejected(UUID eventId);

    void sendChangesRequested(UUID eventId);

    void sendAmendmentsCorrected(UUID eventId);

    void sendDelegationNotice(UUID eventId);

    /**
     * Copie dédiée au créateur confirmant la validation de son événement.
     */
    void sendEventValidatedToCreator(UUID eventId);

    /**
     * Notifie la cellule protocole qu'un événement vient d'être validé.
     */
    void sendEventValidatedToProtocole(UUID eventId);

    /**
     * Informe le créateur que le délégué sollicité a décliné la délégation.
     */
    void sendDelegationDeclined(UUID eventId);

    /**
     * Relance CGE/ADMIN quand une validation approche ou atteint son échéance.
     * @param pourcentage 50, 80 ou 100 (échéance atteinte)
     */
    void sendRelanceValidation(UUID eventId, int pourcentage);

    /**
     * Relance d'escalade (échéance dépassée) : créateur + protocole en plus du CGE/ADMIN.
     */
    void sendRelanceValidationEscalade(UUID eventId);

    /**
     * Invitation à devenir gestionnaire d'un espace agenda — lien signé à usage unique,
     * simple prise de connaissance (pas de refus possible, voir MembreEspaceService).
     */
    void sendEspaceInvitation(UUID membreEspaceId);

    /**
     * Envoyé à la création d'un compte : identifiant + mot de passe temporaire, à
     * changer obligatoirement à la première connexion.
     */
    void sendAccountCreatedEmail(String email, String username, String temporaryPassword);

    void sendEventCancellation(UUID eventId, String reason);

    void sendEventPostponement(UUID eventId);

    /**
     * Alerte technique (espace disque bas, échec de sauvegarde, etc.) envoyée aux
     * comptes ADMIN par le job de surveillance périodique (HealthMonitorScheduler).
     */
    void sendSystemAlert(String subject, String message);

    /**
     * Retente l'envoi de tous les emails en attente de la file d'attente (outbox)
     * dont l'heure de nouvelle tentative est passée. Appelé périodiquement par
     * un job planifié — ne doit normalement pas être invoqué manuellement.
     */
    void processOutbox();
}