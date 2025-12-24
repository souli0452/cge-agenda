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
public class DashboardStatsDto {

    // Statistiques générales
    private long totalEvents;
    private int upcomingEventsCount;
    private long totalParticipants;

    // Répartition par statut
    private Map<EventStatus, Long> eventsByStatus;

    // Répartition par type
    private Map<EventType, Long> eventsByType;

    // Événements à venir (7 jours)
    private List<EventDto> upcomingEvents;

    // Top 3 types d'événements
    private List<TypeStatsDto> topEventTypes;
}