package gov.bf.ascelc.cge_agenda.service;

import gov.bf.ascelc.cge_agenda.dto.NotificationDto;
import gov.bf.ascelc.cge_agenda.enums.NotificationType;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    /**
     * Crée une notification pour un destinataire. Ne lève jamais d'exception :
     * un échec de notification ne doit pas faire échouer l'action métier qui la déclenche.
     */
    void notifier(String destinataireEmail, NotificationType type, UUID eventId, String message);

    /**
     * Notifications de l'utilisateur actuellement connecté, les plus récentes d'abord.
     */
    List<NotificationDto> getMesNotifications();

    long countNonLues();

    void marquerLue(UUID id);

    void marquerToutesLues();
}
