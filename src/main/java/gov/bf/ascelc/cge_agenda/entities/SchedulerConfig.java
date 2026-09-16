package gov.bf.ascelc.cge_agenda.entities;

import gov.bf.ascelc.cge_agenda.abstracts.AuditEntity;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "scheduler_config")
public class SchedulerConfig extends AuditEntity {

    @Column(name = "reminder_enabled", nullable = false)
    private boolean reminderEnabled;

    @Column(name = "send_hour", nullable = false)
    private int sendHour;

    @ElementCollection
    @CollectionTable(name = "scheduler_config_reminder_days", joinColumns = @JoinColumn(name = "scheduler_config_id"))
    @Column(name = "days_until")
    @Builder.Default
    private List<Integer> reminderDays = new ArrayList<>();
}
