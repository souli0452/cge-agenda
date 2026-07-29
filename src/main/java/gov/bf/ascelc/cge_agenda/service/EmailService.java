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

    void sendEventCancellation(UUID eventId, String reason);

    void sendEventPostponement(UUID eventId);

    /**
     * Retente l'envoi de tous les emails en attente de la file d'attente (outbox)
     * dont l'heure de nouvelle tentative est passée. Appelé périodiquement par
     * un job planifié — ne doit normalement pas être invoqué manuellement.
     */
    void processOutbox();
}