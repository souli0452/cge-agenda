package gov.bf.ascelc.cge_agenda.entities;

import gov.bf.ascelc.cge_agenda.abstracts.AuditEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "schedule")
public class Schedule extends AuditEntity {

    @Column(name = "dateJour", nullable = false)
    private LocalDate DateJour;

    @Column(name = "start_time", nullable = false)
    private LocalTime start_time;

    @Column(name = "end_time", nullable = false)
    private LocalTime end_time;

    @Column(name = "address")
    private String address;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;



}
