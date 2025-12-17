package gov.bf.ascelc.cge_agenda.entities;

import gov.bf.ascelc.cge_agenda.abstracts.AuditEntity;
import gov.bf.ascelc.cge_agenda.enums.EventStatus;
import gov.bf.ascelc.cge_agenda.enums.EventType;
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
    @Column(name = "statut", nullable = false)
    private EventStatus statut;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private EventType type;

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Schedule> schedules = new HashSet<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<File> files = new HashSet<>();

    @OneToMany(mappedBy = "event", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Participant> participants = new HashSet<>();



    /**
     * Méthode utilitaire pour vérifier si l'événement est multi-jours
     */
    @Transient
    public boolean isMultiJours() {
        return startDate != null && endDate != null && !startDate.equals(endDate);
    }

    /**
     * Méthode utilitaire pour obtenir le nombre de jours
     */
    @Transient
    public long getNombreJours() {
        if (startDate == null || endDate == null) return 0;
        return java.time.temporal.ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

}
