package gov.bf.ascelc.cge_agenda.entities;

import gov.bf.ascelc.cge_agenda.abstracts.AuditEntity;
import gov.bf.ascelc.cge_agenda.enums.EventStatus;
import gov.bf.ascelc.cge_agenda.enums.EventType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "event")
public class Event extends AuditEntity {

    @Column(name = "title", nullable = false)
    private String title;

    @Column(name = "description",columnDefinition = "TEXT")
    private String description;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Column(name = "meeting_link", length = 500)
    private String meetingLink;

    @Column(name = "pays")
    private String pays;

    @Column(name = "ville")
    private String ville;

    @Column(name = "lieu_type", length = 30)
    private String lieuType;

    @Column(name = "salle", length = 150)
    private String salle;

    @Column(name = "nom_lieu", length = 200)
    private String nomLieu;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false,length = 50)
    private EventStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false,length = 50)
    private EventType type;

    @Column(name = "deleted", nullable = false)
    @Builder.Default
    private boolean deleted = false;

    // ==========================================
    // WORKFLOW DE VALIDATION CGE
    // ==========================================
    @Column(name = "validation_comment", columnDefinition = "TEXT")
    private String validationComment;

    @Column(name = "rejection_reason", columnDefinition = "TEXT")
    private String rejectionReason;

    @Column(name = "change_suggestions", columnDefinition = "TEXT")
    private String changeSuggestions;

    @Column(name = "creator_email")
    private String creatorEmail;

    @Column(name = "creator_username")
    private String creatorUsername;

    @Column(name = "creator_role")
    private String creatorRole;

    @Column(name = "delegue_nom")
    private String delegueNom;

    @Column(name = "delegue_email")
    private String delegueEmail;

    @Column(name = "delegue_motif", columnDefinition = "TEXT")
    private String delegueMotif;

    @Column(name = "est_delegue", nullable = false)
    @Builder.Default
    private boolean estDelegue = false;

    @Column(name = "delegue_date")
    private java.time.LocalDateTime delegueDate;

    @Column(name = "delegue_par_email")
    private String delegueParEmail;

    // ==========================================
    // COMPTE-RENDU DE RÉUNION (disponible une fois TERMINE)
    // ==========================================
    @Column(name = "compte_rendu_points", columnDefinition = "TEXT")
    private String compteRenduPoints;

    @Column(name = "compte_rendu_decisions", columnDefinition = "TEXT")
    private String compteRenduDecisions;

    @Column(name = "compte_rendu_actions", columnDefinition = "TEXT")
    private String compteRenduActions;

    @Column(name = "compte_rendu_redige_par")
    private String compteRenduRedigePar;

    @Column(name = "compte_rendu_date")
    private java.time.LocalDateTime compteRenduDate;

    /**
     * Relation One-to-Many avec Schedule
     * Un événement peut avoir plusieurs horaires
     */
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<Schedule> schedules = new HashSet<>();

    /**
     * Relation One-to-Many avec File
     * Un événement peut avoir plusieurs fichiers
     */
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<File> files = new HashSet<>();



    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ParticipantEvent> participantEvents = new HashSet<>();


    public void addSchedule(Schedule schedule) {
        schedules.add(schedule);
        schedule.setEvent(this);
    }

    public void removeSchedule(Schedule schedule) {
        schedules.remove(schedule);
        schedule.setEvent(null);
    }

    public void addFile(File file) {
        files.add(file);
        file.setEvent(this);
    }

    public void removeFile(File file) {
        files.remove(file);
        file.setEvent(null);
    }

}
