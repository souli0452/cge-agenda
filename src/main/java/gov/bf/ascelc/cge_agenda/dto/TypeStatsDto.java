package gov.bf.ascelc.cge_agenda.dto;

import gov.bf.ascelc.cge_agenda.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TypeStatsDto {
    private EventType type;
    private long count;
    private double percentage;
}