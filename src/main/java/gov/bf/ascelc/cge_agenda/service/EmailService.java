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
}