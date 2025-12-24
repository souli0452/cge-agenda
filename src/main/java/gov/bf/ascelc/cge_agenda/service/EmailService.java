package gov.bf.ascelc.cge_agenda.service;

import gov.bf.ascelc.cge_agenda.entities.Event;
import gov.bf.ascelc.cge_agenda.entities.File;

/**
 * Service de gestion des emails
 */
public interface EmailService {

    /**
     * Envoyer un rappel d'événement (J-7 ou J-1)
     * @param event Événement concerné
     * @param daysUntil Nombre de jours avant l'événement
     */
    void sendEventReminder(Event event, int daysUntil);

    /**
     * Notifier les participants d'une modification d'événement
     * @param event Événement modifié
     */
    void sendEventUpdateNotification(Event event);

    /**
     * Notifier les participants d'un nouveau document
     * @param event Événement concerné
     * @param file Fichier ajouté
     */
    void sendNewDocumentNotification(Event event, File file);
}