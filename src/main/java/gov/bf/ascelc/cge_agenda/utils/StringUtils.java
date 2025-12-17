package gov.bf.ascelc.cge_agenda.utils;

public class StringUtils {
    private StringUtils() {
        // Constructeur privé
    }

    /**
     * Capitalise la première lettre d'une chaîne
     */
    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    /**
     * Capitalise la première lettre de chaque mot
     */
    public static String capitalizeWords(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        String[] words = str.split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            result.append(capitalize(word)).append(" ");
        }
        return result.toString().trim();
    }

    /**
     * Tronque une chaîne à une longueur maximale et ajoute "..."
     */
    public static String truncate(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    /**
     * Nettoie une chaîne (trim + supprime les espaces multiples)
     */
    public static String clean(String str) {
        if (str == null) {
            return null;
        }
        return str.trim().replaceAll("\\s+", " ");
    }

    /**
     * Génère des initiales à partir d'un nom complet
     * Ex: "Jean Dupont" -> "JD"
     */
    public static String getInitials(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return "";
        }
        String[] parts = fullName.trim().split("\\s+");
        StringBuilder initials = new StringBuilder();
        for (String part : parts) {
            if (!part.isEmpty()) {
                initials.append(part.charAt(0));
            }
        }
        return initials.toString().toUpperCase();
    }
}
