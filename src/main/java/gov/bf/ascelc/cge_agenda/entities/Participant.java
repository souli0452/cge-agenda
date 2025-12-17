package gov.bf.ascelc.cge_agenda.entities;

import gov.bf.ascelc.cge_agenda.abstracts.AuditEntity;
import gov.bf.ascelc.cge_agenda.enums.ParticipantType;
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

public class Participant extends AuditEntity{

    @Column(name = "last_name", nullable = false)
    private String last_name;

    @Column(name = "first_name", nullable = false)
    private String first_name;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "phone_number")
    private String  phone_number;

    @Column(name = "job_title")
    private String  job_title;

    @Column(name = "organization")
    private String  organization;

    @Enumerated(EnumType.STRING)
    @Column(name = "participantType", nullable = false)
    private ParticipantType participantType;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false, foreignKey = @ForeignKey(name = "fk_participant_event"))
    private Event event;

}
