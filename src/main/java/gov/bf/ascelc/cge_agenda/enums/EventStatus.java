package gov.bf.ascelc.cge_agenda.enums;

public enum EventStatus {

    BROUILLON("Brouillon"),
    EN_ATTENTE_VALIDATION("En attente de validation"),
    A_CORRIGER("À corriger"),
    PLANIFIE("Planifié"),
    EN_COURS("En cours"),
    TERMINE("Terminé"),
    ANNULER("Annulé"),
    REPORTER("Reporté"),
    REJETE("Rejeté");


    private final String label;

    EventStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
