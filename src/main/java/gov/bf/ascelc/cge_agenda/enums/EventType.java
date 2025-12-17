package gov.bf.ascelc.cge_agenda.enums;

public enum EventType {
    REUNION("Réunion"),
    CONFERENCE("Conférence"),
    ATELIER("Atelier"),
    SEMINAIRE("Séminaire"),
    FORMATION("Formation"),
    MISSION("Mission"),
    AUTRE("Autre");


    private final String label;

    EventType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
