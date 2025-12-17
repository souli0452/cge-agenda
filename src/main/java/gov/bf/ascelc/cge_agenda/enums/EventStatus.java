package gov.bf.ascelc.cge_agenda.enums;

public enum EventStatus {

    PLANIFIE("Planifié"),
    EN_COURS("En cours"),
    TERMINE("Terminé"),
    ANNULER("Annulé"),
    REPORTER("Reporté");


    private final String label;

    EventStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
