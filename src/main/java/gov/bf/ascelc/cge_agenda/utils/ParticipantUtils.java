package gov.bf.ascelc.cge_agenda.utils;

import gov.bf.ascelc.cge_agenda.entities.Participant;
import gov.bf.ascelc.cge_agenda.enums.ParticipantType;

import java.util.List;
import java.util.stream.Collectors;

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

    /**
     * Filtre les participants internes d'une liste
     */
    public static List<Participant> filterInternalParticipants(List<Participant> participants) {
        if (participants == null) {
            return List.of();
        }
        return participants.stream()
                .filter(p -> p.getParticipantType() == ParticipantType.INTERNE)
                .collect(Collectors.toList());
    }

    /**
     * Filtre les participants externes d'une liste
     */
    public static List<Participant> filterExternalParticipants(List<Participant> participants) {
        if (participants == null) {
            return List.of();
        }
        return participants.stream()
                .filter(p -> p.getParticipantType() == ParticipantType.EXTERNE)
                .collect(Collectors.toList());
    }

    /**
     * Compte le nombre de participants internes
     */
    public static long countInternalParticipants(List<Participant> participants) {
        return filterInternalParticipants(participants).size();
    }

    /**
     * Compte le nombre de participants externes
     */
    public static long countExternalParticipants(List<Participant> participants) {
        return filterExternalParticipants(participants).size();
    }

    /**
     * Génère une liste d'emails de participants
     */
    public static List<String> extractEmails(List<Participant> participants) {
        if (participants == null) {
            return List.of();
        }
        return participants.stream()
                .map(Participant::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    /**
     * Vérifie si un email existe déjà dans une liste de participants
     */
    public static boolean emailExists(List<Participant> participants, String email) {
        if (participants == null || email == null) {
            return false;
        }
        return participants.stream()
                .anyMatch(p -> email.equalsIgnoreCase(p.getEmail()));
    }
}
