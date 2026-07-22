package gov.bf.ascelc.cge_agenda.service;

import gov.bf.ascelc.cge_agenda.dto.EventDto;
import gov.bf.ascelc.cge_agenda.dto.ParticipantDto;
import gov.bf.ascelc.cge_agenda.enums.EventStatus;
import gov.bf.ascelc.cge_agenda.enums.EventType;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public interface EventService {
    /**
     * Créer un événement avec participants et horaires
     * Vérifie la disponibilité des participants
     */
    EventDto create(EventDto dto);
    void sendTestReminder(UUID eventId);

    /**
     * Mise à jour d'un événement existant
     */
    EventDto update(UUID id, EventDto eventDto);

    /**
     * Récupérer tous les événements
     */
    List<EventDto> allEvents();

    /**
     * Récupérer un événement par son ID
     */
    EventDto getEventById(UUID id);

    /**
     * Supprimer un événement
     */
    void delete(UUID id);

    /**
     * Annuler un événement (change le statut à ANNULER)
     */
    EventDto cancelEvent(UUID id, String reason);

    /**
     * Reporter un événement (change le statut à REPORTER)
     */
    EventDto postponeEvent(UUID id, LocalDate newStartDate, LocalDate newEndDate);

    /**
     * Rechercher des événements par critères
     */
    List<EventDto> searchEvents(String keyword, EventType type, EventStatus status,
                                LocalDate startDate, LocalDate endDate);

    /**
     * Récupérer les événements d'un mois donné
     */
    List<EventDto> getEventsByMonth(int year, int month);

    /**
     * Récupérer les événements d'une période
     */
    List<EventDto> getEventsByDateRange(LocalDate startDate, LocalDate endDate);

    /**
     * Ajouter un participant à un événement existant
     */
    EventDto addParticipant(UUID eventId, ParticipantDto participantDto);

    /**
     * Retirer un participant d'un événement
     */
    EventDto removeParticipant(UUID eventId, UUID participantId);

    /**
     * Récupérer les participants d'un événement
     */
    List<ParticipantDto> getEventParticipants(UUID eventId);

    /**
     * Importer des participants depuis un fichier (CSV/Excel)
     */
    List<ParticipantDto> importParticipants(UUID eventId, byte[] fileContent, String fileType);

    /**
     * Générer la liste d'émargement (feuille de présence)
     */
    byte[] generateAttendanceSheet(UUID eventId);

    // Dans EventService
    boolean isParticipantAvailable(UUID participantId, LocalDate date,
                                   LocalTime startTime, LocalTime endTime);

    List<EventDto> getEventsByParticipant(UUID participantId);

    void updateStatusOnly(UUID id, String status);

    /**
     * Récupérer les événements mis à la corbeille (suppression douce)
     */
    List<EventDto> getCorbeille();

    /**
     * Restaurer un événement depuis la corbeille
     */
    EventDto restoreEvent(UUID id);

    /**
     * Supprimer définitivement un événement (et ses fichiers/horaires/participants)
     */
    void deleteEventPermanently(UUID id);

    // ==========================================
    // WORKFLOW DE VALIDATION CGE
    // ==========================================

    /**
     * Soumettre un brouillon à la validation CGE (BROUILLON → EN_ATTENTE_VALIDATION)
     */
    EventDto submitDraft(UUID id);

    /**
     * Valider un événement (EN_ATTENTE_VALIDATION → PLANIFIE), envoie les invitations
     */
    EventDto validateEvent(UUID id, String comment);

    /**
     * Rejeter un événement (EN_ATTENTE_VALIDATION ou A_CORRIGER → REJETE)
     */
    EventDto rejectEvent(UUID id, String reason);

    /**
     * Demander des modifications au créateur (EN_ATTENTE_VALIDATION → A_CORRIGER)
     */
    EventDto requestChanges(UUID id, String suggestions);

    /**
     * Déléguer la participation à un événement PLANIFIE/EN_COURS
     */
    EventDto delegateParticipation(UUID id, String delegueNom, String delegueEmail, String delegueMotif);

    /**
     * Ajouter/modifier une observation CGE sur un événement PLANIFIE/EN_COURS
     */
    EventDto addObservation(UUID id, String observation);

    /**
     * Rédiger/modifier le compte-rendu officiel d'une réunion TERMINE.
     * Autorisé au CGE, à l'ADMIN, ou au créateur de l'événement.
     */
    EventDto saveCompteRendu(UUID id, String points, String decisions, String actions);

}
