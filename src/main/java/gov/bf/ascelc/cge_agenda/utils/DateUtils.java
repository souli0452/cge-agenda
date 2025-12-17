package gov.bf.ascelc.cge_agenda.utils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class DateUtils {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MM-yyyy' à 'HH:mm:ss");

    private DateUtils() {
        // Constructeur privé pour empêcher l'instanciation
    }

    /**
     * Calcule le nombre de jours entre deux dates (inclus)
     *
     * @param startDate Date de début
     * @param endDate Date de fin
     * @return Nombre de jours
     */
    public static long calculateDaysBetween(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return 0;
        }
        return ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    /**
     * Vérifie si une date est entre deux dates (inclus)
     *
     * @param date Date à vérifier
     * @param startDate Date de début
     * @param endDate Date de fin
     * @return true si la date est dans la période
     */
    public static boolean isDateBetween(LocalDate date, LocalDate startDate, LocalDate endDate) {
        if (date == null || startDate == null || endDate == null) {
            return false;
        }
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }

    /**
     * Vérifie si deux périodes se chevauchent
     *
     * @param start1 Début période 1
     * @param end1 Fin période 1
     * @param start2 Début période 2
     * @param end2 Fin période 2
     * @return true si les périodes se chevauchent
     */
    public static boolean periodsOverlap(LocalDate start1, LocalDate end1,
                                         LocalDate start2, LocalDate end2) {
        if (start1 == null || end1 == null || start2 == null || end2 == null) {
            return false;
        }
        return !end1.isBefore(start2) && !start1.isAfter(end2);
    }

    /**
     * Formate une date au format dd-MM-yyyy
     */
    public static String formatDate(LocalDate date) {
        return date != null ? date.format(DATE_FORMATTER) : "";
    }

    /**
     * Formate une date-heure au format dd-MM-yyyy à HH:mm:ss
     */
    public static String formatDateTime(LocalDateTime dateTime) {
        return dateTime != null ? dateTime.format(DATETIME_FORMATTER) : "";
    }

    /**
     * Vérifie si une date est dans le passé
     */
    public static boolean isInPast(LocalDate date) {
        return date != null && date.isBefore(LocalDate.now());
    }

    /**
     * Vérifie si une date est dans le futur
     */
    public static boolean isInFuture(LocalDate date) {
        return date != null && date.isAfter(LocalDate.now());
    }

    /**
     * Vérifie si une date est aujourd'hui
     */
    public static boolean isToday(LocalDate date) {
        return date != null && date.equals(LocalDate.now());
    }
}
