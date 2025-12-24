package gov.bf.ascelc.cge_agenda.utils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.regex.Pattern;

public class ValidationUtils {
    /**
     * Vérifie qu'une plage de dates est valide
     */
    public static boolean isValidDateRange(LocalDate startDate, LocalDate endDate) {
        if (startDate == null || endDate == null) {
            return false;
        }
        return !endDate.isBefore(startDate);
    }

    /**
     * Vérifie qu'une plage horaire est valide
     */
    public static boolean isValidTimeRange(LocalTime startTime, LocalTime endTime) {
        if (startTime == null || endTime == null) {
            return false;
        }
        return endTime.isAfter(startTime);
    }

    /**
     * Vérifie qu'un email est valide (validation simple)
     */
    public static boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        String emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}$";
        Pattern pattern = Pattern.compile(emailRegex);
        return pattern.matcher(email).matches();
    }

    /**
     * Vérifie qu'une chaîne n'est pas vide après trim
     */
    public static boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
