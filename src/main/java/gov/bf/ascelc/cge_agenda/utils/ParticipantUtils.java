package gov.bf.ascelc.cge_agenda.utils;

import gov.bf.ascelc.cge_agenda.entities.Participant;

public class ParticipantUtils {
    private ParticipantUtils() {
        // Constructeur
    }

    /**
     * Obtient le nom complet d'un participant
     *
     * @param participant Participant
     * @return Nom complet (Prénom Nom)
     */
    public static String getFullName(Participant participant) {
        if (participant == null) {
            return "";
        }
        String firstName = participant.getFirstName() != null ? participant.getFirstName() : "";
        String lastName = participant.getLastName() != null ? participant.getLastName() : "";
        return (firstName + " " + lastName).trim();
    }

    /**
     * Obtient le nom complet formaté (Nom, Prénom)
     */
    public static String getFormattedName(Participant participant) {
        if (participant == null) {
            return "";
        }
        String firstName = participant.getFirstName() != null ? participant.getFirstName() : "";
        String lastName = participant.getLastName() != null ? participant.getLastName() : "";
        return (lastName + ", " + firstName).trim();
    }
}
