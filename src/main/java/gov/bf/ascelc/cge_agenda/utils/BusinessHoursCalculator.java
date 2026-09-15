package gov.bf.ascelc.cge_agenda.utils;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

/**
 * Calcule des échéances en heures ouvrables (lundi-vendredi, hors jours fériés). Utilitaire
 * pur, sans dépendance Spring — la plage horaire et la liste des jours fériés sont fournies
 * par l'appelant (heures configurables en admin, jours fériés chargés depuis la base).
 */
public final class BusinessHoursCalculator {

    public static final LocalTime DEFAULT_BUSINESS_START = LocalTime.of(7, 30);
    public static final LocalTime DEFAULT_BUSINESS_END = LocalTime.of(17, 0);

    private BusinessHoursCalculator() {}

    public static boolean isBusinessDay(LocalDate date, Set<LocalDate> feries) {
        return date.getDayOfWeek() != DayOfWeek.SATURDAY
                && date.getDayOfWeek() != DayOfWeek.SUNDAY
                && !feries.contains(date);
    }

    public static boolean isBusinessMoment(LocalDateTime moment, Set<LocalDate> feries,
                                            LocalTime businessStart, LocalTime businessEnd) {
        return isBusinessDay(moment.toLocalDate(), feries)
                && !moment.toLocalTime().isBefore(businessStart)
                && moment.toLocalTime().isBefore(businessEnd);
    }

    /**
     * Ajoute {@code heures} heures ouvrables à {@code debut}. Si {@code debut} tombe en
     * dehors des heures ouvrables, le calcul repart du prochain créneau ouvré.
     */
    public static LocalDateTime ajouterHeuresOuvrables(LocalDateTime debut, long heures, Set<LocalDate> feries,
                                                         LocalTime businessStart, LocalTime businessEnd) {
        LocalDateTime current = alignerSurCreneauOuvre(debut, feries, businessStart, businessEnd);
        long minutesRestantes = heures * 60;

        while (minutesRestantes > 0) {
            LocalDateTime finJournee = current.toLocalDate().atTime(businessEnd);
            long minutesDisponibles = Duration.between(current, finJournee).toMinutes();

            if (minutesRestantes <= minutesDisponibles) {
                return current.plusMinutes(minutesRestantes);
            }
            minutesRestantes -= minutesDisponibles;
            current = prochainJourOuvre(current.toLocalDate(), feries).atTime(businessStart);
        }
        return current;
    }

    private static LocalDateTime alignerSurCreneauOuvre(LocalDateTime dt, Set<LocalDate> feries,
                                                          LocalTime businessStart, LocalTime businessEnd) {
        LocalDate date = dt.toLocalDate();
        if (!isBusinessDay(date, feries)) {
            return prochainJourOuvre(date, feries).atTime(businessStart);
        }
        if (dt.toLocalTime().isBefore(businessStart)) {
            return date.atTime(businessStart);
        }
        if (!dt.toLocalTime().isBefore(businessEnd)) {
            return prochainJourOuvre(date, feries).atTime(businessStart);
        }
        return dt;
    }

    private static LocalDate prochainJourOuvre(LocalDate apres, Set<LocalDate> feries) {
        LocalDate d = apres.plusDays(1);
        while (!isBusinessDay(d, feries)) {
            d = d.plusDays(1);
        }
        return d;
    }
}
