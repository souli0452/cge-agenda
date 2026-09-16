package gov.bf.ascelc.cge_agenda.entities;

import gov.bf.ascelc.cge_agenda.abstracts.AuditEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@Entity
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "backup_config")
public class BackupConfig extends AuditEntity {

    @Column(name = "auto_enabled", nullable = false)
    private boolean autoEnabled;

    @Column(name = "backup_hour", nullable = false)
    private int backupHour;

    @Column(name = "backup_minute", nullable = false)
    private int backupMinute;

    @Column(name = "retention_count", nullable = false)
    private int retentionCount;
}
