package gov.bf.ascelc.cge_agenda.entities;

import gov.bf.ascelc.cge_agenda.abstracts.AuditEntity;
import gov.bf.ascelc.cge_agenda.enums.ParticipantType;
import gov.bf.ascelc.cge_agenda.utils.ParticipantUtils;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "participant")

public class Participant extends AuditEntity {

    @Column(name = "lastName", nullable = false, length = 100)
    private String lastName;

    @Column(name = "firstName", nullable = false, length = 100)
    private String firstName;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "phoneNumber", length = 20)
    private String phoneNumber;

    @Column(name = "jobTitle", length = 255)
    private String jobTitle;

    @Column(name = "organization", length = 255)
    private String organization;

    @Enumerated(EnumType.STRING)
    @Column(name = "participant_type", nullable = false, length = 50)
    private ParticipantType participantType;

    /**
     * Relation Many-to-One avec Event
     * Un participant appartient à UN SEUL événement
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false, foreignKey = @ForeignKey(name = "fk_participant_event"))
    private Event event;

    /**
     * Obtient le nom complet du participant
     */
    @Transient
    public String getFullName() {
        return ParticipantUtils.getFullName(this);
    }

    /**
     * Obtient le nom formaté (Nom, Prénom)
     */
    @Transient
    public String getFormattedName() {
        return ParticipantUtils.getFormattedName(this);
    }

    /**
     * Vérifie si le participant est interne
     */
    @Transient
    public boolean isInterne() {
        return participantType == ParticipantType.INTERNE;
    }

    /**
     * Vérifie si le participant est externe
     */
    @Transient
    public boolean isExterne() {
        return participantType == ParticipantType.EXTERNE;
    }
}
