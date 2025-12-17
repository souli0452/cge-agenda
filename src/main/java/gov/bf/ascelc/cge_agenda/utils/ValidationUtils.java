package gov.bf.ascelc.cge_agenda.utils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.regex.Pattern;

public class ValidationUtils {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^\\+?[0-9\\s-]{8,}$");

    private ValidationUtils() {
        // Constructeur privé
    }

    /**
     * Valide un email
     */
    public static boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Valide un numéro de téléphone
     */
    public static boolean isValidPhoneNumber(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * Valide que la date de fin est >= date de début
     */
    public static boolean isValidDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return false;
        }
        return !endDate.isBefore(startDate);
    }

    /**
     * Valide que l'heure de fin est > heure de début
     */
    public static boolean isValidTimeRange(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            return false;
        }
        return endTime.isAfter(startTime);
    }

    /**
     * Valide qu'une chaîne n'est pas vide
     */
    public static boolean isNotBlank(String str) {
        return str != null && !str.trim().isEmpty();
    }

    /**
     * Valide qu'une URL est bien formée
     */
    public static boolean isValidUrl(String url) {
        if (url == null || url.isBlank()) {
            return true; // URL optionnelle
        }
        return url.startsWith("http://") || url.startsWith("https://");
    }
}
