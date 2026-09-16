package gov.bf.ascelc.cge_agenda.dto;

import gov.bf.ascelc.cge_agenda.enums.EventType;
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
public class EventTypeSlaDto {
    private EventType eventType;
    private Integer delaiHeuresOuvrables;
    private Integer delaiAvantEvenementHeures;
}
