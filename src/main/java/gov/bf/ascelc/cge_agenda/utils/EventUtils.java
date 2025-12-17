package gov.bf.ascelc.cge_agenda.utils;

import gov.bf.ascelc.cge_agenda.entities.Event;
import gov.bf.ascelc.cge_agenda.enums.EventStatus;

import java.time.LocalDate;

public class EventUtils {
    private EventUtils() {
        // Constructeur
    }

    /**
     * Vérifie si un événement est multi-jours
     *
     * @param event Événement à vérifier
     * @return true si l'événement dure plusieurs jours
     */
    public static boolean isMultiDayEvent(Event event) {
        if (event == null || event.getStartDate() == null || event.getEndDate() == null) {
            return false;
        }
        return !event.getStartDate().equals(event.getEndDate());
    }

    /**
     * Vérifie si un événement est multi-jours (avec dates directement)
     */
    public static boolean isMultiDayEvent(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return false;
        }
        return !startDate.equals(endDate);
    }

    /**
     * Calcule le nombre de jours d'un événement
     *
     * @param event Événement
     * @return Nombre de jours
     */
    public static long getEventDuration(Event event) {
        if (event == null) {
            return 0;
        }
        return DateUtils.calculateDaysBetween(event.getStartDate(), event.getEndDate());
    }

    /**
     * Vérifie si un événement se déroule à une date donnée
     *
     * @param event Événement
     * @param date Date à vérifier
     * @return true si l'événement se déroule ce jour-là
     */
    public static boolean eventOccursOnDate(Event event, LocalDate date) {
        if (event == null || date == null) {
            return false;
        }
        return DateUtils.isDateBetween(date, event.getStartDate(), event.getEndDate());
    }

    /**
     * Vérifie si un événement est en cours
     *
     * @param event Événement
     * @return true si l'événement est en cours aujourd'hui
     */
    public static boolean isEventOngoing(Event event) {
        if (event == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        return DateUtils.isDateBetween(today, event.getStartDate(), event.getEndDate()) &&
                (event.getStatus() == EventStatus.PLANIFIE || event.getStatus() == EventStatus.EN_COURS);
    }

    /**
     * Vérifie si un événement est terminé
     */
    public static boolean isEventFinished(Event event) {
        if (event == null) {
            return false;
        }
        return event.getStatus() == EventStatus.TERMINE ||
                (event.getEndDate() != null && event.getEndDate().isBefore(LocalDate.now()));
    }

    /**
     * Vérifie si un événement est à venir
     */
    public static boolean isEventUpcoming(Event event) {
        if (event == null) {
            return false;
        }
        return event.getStatus() == EventStatus.PLANIFIE &&
                event.getStartDate() != null &&
                event.getStartDate().isAfter(LocalDate.now());
    }

    /**
     * Génère un titre court pour un événement multi-jours
     * Ex: "Formation Excel (17-30 mars)"
     */
    public static String generateShortTitle(Event event) {
        if (event == null || event.getTitle() == null) {
            return "";
        }

        if (!isMultiDayEvent(event)) {
            return event.getTitle();
        }

        String dates = DateUtils.formatDate(event.getStartDate()) + " - " +
                DateUtils.formatDate(event.getEndDate());
        return event.getTitle() + " (" + dates + ")";
    }
}
