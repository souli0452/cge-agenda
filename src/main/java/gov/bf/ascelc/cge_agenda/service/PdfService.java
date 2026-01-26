package gov.bf.ascelc.cge_agenda.service;

import gov.bf.ascelc.cge_agenda.dto.EventDto;
import gov.bf.ascelc.cge_agenda.enums.EventStatus;
import gov.bf.ascelc.cge_agenda.enums.EventType;

import java.time.LocalDate;
import java.util.List;

/**
 * Service de génération de documents PDF pour les événements
 *
 * @author ASCELC CGE
 */
public interface PdfService {

    // ==========================================
    // EXPORTS PAR PÉRIODE FIXE
    // ==========================================

    /**
     * Génère un PDF des événements du jour
     *
     * @param date Date du jour
     * @return PDF bytes
     */
    byte[] generateDailyReport(LocalDate date);

    /**
     * Génère un PDF des événements de la semaine
     * La semaine commence le lundi et se termine le dimanche
     *
     * @param date N'importe quel jour de la semaine (sera converti en lundi-dimanche)
     * @return PDF bytes
     */
    byte[] generateWeeklyReport(LocalDate date);

    /**
     * Génère un PDF des événements du mois
     *
     * @param year Année
     * @param month Mois (1-12)
     * @return PDF bytes
     */
    byte[] generateMonthlyReport(int year, int month);

    /**
     * Génère un PDF des événements de l'année
     *
     * @param year Année
     * @return PDF bytes
     */
    byte[] generateYearlyReport(int year);

    // ==========================================
    // EXPORT PAR PLAGE DE DATES PERSONNALISÉE
    // ==========================================

    /**
     * Génère un PDF des événements pour une plage de dates personnalisée
     * C'est la méthode la plus flexible - peut être utilisée pour n'importe quelle période
     *
     * @param startDate Date de début (inclusive)
     * @param endDate Date de fin (inclusive)
     * @return PDF bytes
     */
    byte[] generateCustomRangeReport(LocalDate startDate, LocalDate endDate);

    /**
     * Génère un PDF des événements avec filtres avancés
     *
     * @param startDate Date de début
     * @param endDate Date de fin
     * @param keyword Mot-clé de recherche (nullable)
     * @param type Type d'événement (nullable)
     * @param status Statut d'événement (nullable)
     * @return PDF bytes
     */
    byte[] generateFilteredReport(
            LocalDate startDate,
            LocalDate endDate,
            String keyword,
            EventType type,
            EventType status
    );

    // ==========================================
    // EXPORT AVEC LISTE D'ÉVÉNEMENTS
    // ==========================================

    /**
     * Génère un PDF à partir d'une liste d'événements déjà filtrée
     * Utile quand le filtrage est fait en amont
     *
     * @param events Liste des événements à inclure dans le PDF
     * @return PDF bytes
     */
    byte[] generateEventsReportPDF(List<EventDto> events);

    /**
     * Génère un PDF avec titre personnalisé
     *
     * @param events Liste des événements
     * @param title Titre du rapport (ex: "Événements de la semaine 4")
     * @param subtitle Sous-titre (ex: "22/01/2026 au 28/01/2026")
     * @return PDF bytes
     */
    byte[] generateCustomReport(List<EventDto> events, String title, String subtitle);

    // ==========================================
    // CALENDRIERS ET DOCUMENTS SPÉCIAUX
    // ==========================================

    /**
     * Génère un calendrier mensuel visuel (vue calendrier)
     *
     * @param year Année
     * @param month Mois
     * @return PDF bytes
     */
    byte[] generateCalendarPDF(int year, int month);

    /**
     * Génère un compte-rendu de réunion pour un événement
     *
     * @param eventId ID de l'événement
     * @return PDF bytes
     */
    byte[] generateMeetingReportPDF(java.util.UUID eventId);

    /**
     * Génère la liste d'émargement pour un événement
     *
     * @param eventId ID de l'événement
     * @return PDF bytes
     */
    byte[] generateAttendanceSheet(java.util.UUID eventId);

    // ==========================================
    // STATISTIQUES ET RAPPORTS ANALYTIQUES
    // ==========================================

    /**
     * Génère un rapport statistique mensuel
     * Inclut des graphiques et statistiques
     *
     * @param year Année
     * @param month Mois
     * @return PDF bytes
     */
    byte[] generateMonthlyStatisticsReport(int year, int month);

    /**
     * Génère un rapport de synthèse pour une période
     *
     * @param startDate Date de début
     * @param endDate Date de fin
     * @return PDF bytes
     */
    byte[] generateSummaryReport(LocalDate startDate, LocalDate endDate);
}