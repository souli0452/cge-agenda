package gov.bf.ascelc.cge_agenda.entities;

import gov.bf.ascelc.cge_agenda.abstracts.AuditEntity;
import gov.bf.ascelc.cge_agenda.enums.ParticipantType;
import gov.bf.ascelc.cge_agenda.utils.ParticipantUtils;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "participant", indexes = {
        @Index(name = "idx_participant_email", columnList = "email"),
        @Index(name = "idx_participant_type", columnList = "participant_type"),
        @Index(name = "idx_participant_structure", columnList = "structure") 
})
public class Participant extends AuditEntity {

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "phone_number", length = 20)
    private String phoneNumber;

    @Column(name = "job_title", length = 255)
    private String jobTitle;


    @Column(name = "structure", length = 255)
    private String structure;

    @Enumerated(EnumType.STRING)
    @Column(name = "participant_type", nullable = false, length = 50)
    private ParticipantType participantType;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    @Column(name = "deleted_at")
    private java.time.LocalDateTime deletedAt;

    @Column(name = "deleted_by", length = 255)
    private String deletedBy;

    @OneToMany(mappedBy = "participant", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ParticipantEvent> participantEvents = new HashSet<>();

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