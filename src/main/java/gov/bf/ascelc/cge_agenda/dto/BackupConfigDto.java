package gov.bf.ascelc.cge_agenda.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupConfigDto {
    private boolean autoEnabled;
    private int backupHour;
    private int backupMinute;
    private int retentionCount;
}
