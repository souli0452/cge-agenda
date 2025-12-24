package gov.bf.ascelc.cge_agenda.dto;

import gov.bf.ascelc.cge_agenda.enums.EventStatus;
import gov.bf.ascelc.cge_agenda.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyReportDto {

    private int month;
    private int year;

    private int totalEvents;
    private int totalParticipants;

    private Map<EventType, Long> eventsByType;
    private Map<EventStatus, Long> eventsByStatus;

    private List<EventDto> events;
}