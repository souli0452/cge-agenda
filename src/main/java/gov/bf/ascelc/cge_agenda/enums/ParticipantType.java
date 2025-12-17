package gov.bf.ascelc.cge_agenda.enums;

public enum ParticipantType {
    INTERNE("Interne"),
    EXTERNE("Externe");

    private final String label;

    ParticipantType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
