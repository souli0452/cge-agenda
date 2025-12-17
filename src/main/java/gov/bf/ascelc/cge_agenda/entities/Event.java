package gov.bf.ascelc.cge_agenda.entities;

import gov.bf.ascelc.cge_agenda.abstracts.AuditEntity;
import gov.bf.ascelc.cge_agenda.enums.EventStatus;
import gov.bf.ascelc.cge_agenda.enums.EventType;
import gov.bf.ascelc.cge_agenda.utils.EventUtils;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
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

    @Column(name = "meetingLink")
    private String meetingLink;

    @Column(name = "pays")
    private String pays;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false,length = 50)
    private EventStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false,length = 50)
    private EventType type;

    /**
     * Relation One-to-Many avec Schedule
     * Un événement peut avoir plusieurs horaires
     */
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Schedule> schedules = new HashSet<>();

    /**
     * Relation One-to-Many avec File
     * Un événement peut avoir plusieurs fichiers
     */
    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<File> files = new HashSet<>();

    /**
     * Relation One-to-Many avec Participant
     * Un événement peut avoir plusieurs participants
     * Chaque participant appartient à UN SEUL événement
     */

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Participant> participants = new HashSet<>();



    /**
     * Vérifie si l'événement est multi-jours
     */
    @Transient
    public boolean isMultiJours() {
        return EventUtils.isMultiDayEvent(this);
    }

    /**
     * Obtient le nombre de jours
     */
    @Transient
    public long getNombreJours() {
        return EventUtils.getEventDuration(this);
    }

    /**
     * Vérifie si l'événement se déroule à une date donnée
     */
    @Transient
    public boolean seDerouleLeJour(LocalDate date) {
        return EventUtils.eventOccursOnDate(this, date);
    }

    /**
     * Vérifie si l'événement est en cours
     */
    @Transient
    public boolean isEnCours() {
        return EventUtils.isEventOngoing(this);
    }

    /**
     * Vérifie si l'événement est terminé
     */
    @Transient
    public boolean isTermine() {
        return EventUtils.isEventFinished(this);
    }

    /**
     * Vérifie si l'événement est à venir
     */
    @Transient
    public boolean isAVenir() {
        return EventUtils.isEventUpcoming(this);
    }

    public void addParticipant(Participant participant) {
        participants.add(participant);
        participant.setEvent(this);
    }

    public void removeParticipant(Participant participant) {
        participants.remove(participant);
        participant.setEvent(null);
    }

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
