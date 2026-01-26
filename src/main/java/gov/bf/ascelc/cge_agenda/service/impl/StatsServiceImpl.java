package gov.bf.ascelc.cge_agenda.service.impl;

import gov.bf.ascelc.cge_agenda.dto.DashboardStatsDto;
import gov.bf.ascelc.cge_agenda.dto.MonthlyReportDto;
import gov.bf.ascelc.cge_agenda.dto.TypeStatsDto;
import gov.bf.ascelc.cge_agenda.entities.Event;
import gov.bf.ascelc.cge_agenda.enums.EventStatus;
import gov.bf.ascelc.cge_agenda.enums.EventType;
import gov.bf.ascelc.cge_agenda.mapper.EventMapper;
import gov.bf.ascelc.cge_agenda.repository.EventRepository;
import gov.bf.ascelc.cge_agenda.repository.ParticipantEventRepository;
import gov.bf.ascelc.cge_agenda.service.StatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final EventRepository eventRepository;
    private final ParticipantEventRepository participantEventRepository;
    private final EventMapper eventMapper;

    @Override
    public DashboardStatsDto getDashboardStats() {
        log.info("📊 Génération des statistiques du dashboard");

        long totalEvents = eventRepository.count();
        long uniqueParticipants = participantEventRepository.countUniqueParticipants();
        long totalInscriptions = participantEventRepository.count();

        // Agrégations par statut
        Map<EventStatus, Long> eventsByStatus = eventRepository.countByStatus().stream()
                .collect(Collectors.toMap(
                        entry -> (EventStatus) entry[0],
                        entry -> ((Number) entry[1]).longValue()
                ));

        // Agrégations par type
        Map<EventType, Long> eventsByType = eventRepository.countByType().stream()
                .collect(Collectors.toMap(
                        entry -> (EventType) entry[0],
                        entry -> ((Number) entry[1]).longValue()
                ));

        // Événements à venir (7 prochains jours)
        LocalDate today = LocalDate.now();
        LocalDate weekFromNow = today.plusDays(7);
        List<Event> upcomingEvents = eventRepository.findUpcomingEvents(today, weekFromNow);

        // Top 3 types
        List<TypeStatsDto> topTypes = eventsByType.entrySet().stream()
                .sorted(Map.Entry.<EventType, Long>comparingByValue().reversed())
                .limit(3)
                .map(entry -> TypeStatsDto.builder()
                        .type(entry.getKey())
                        .count(entry.getValue())
                        .percentage(totalEvents > 0 ? (double) entry.getValue() / totalEvents * 100 : 0)
                        .build())
                .toList();

        log.info("✅ Dashboard généré : {} événements, {} participants uniques, {} inscriptions",
                totalEvents, uniqueParticipants, totalInscriptions);

        return DashboardStatsDto.builder()
                .totalEvents(totalEvents)
                .totalParticipants(uniqueParticipants)
                .totalInscriptions(totalInscriptions)
                .upcomingEventsCount(upcomingEvents.size())
                .eventsByStatus(eventsByStatus)
                .eventsByType(eventsByType)
                .upcomingEvents(eventMapper.toDtos(upcomingEvents))
                .topEventTypes(topTypes)
                .build();
    }

    @Override
    public MonthlyReportDto getMonthlyReport(int year, int month) {
        log.info("📅 Génération du rapport mensuel : {}/{}", month, year);

        YearMonth ym = YearMonth.of(year, month);
        LocalDate startOfMonth = ym.atDay(1);
        LocalDate endOfMonth = ym.atEndOfMonth();

        List<Event> monthEvents = eventRepository.findByMonth(startOfMonth, endOfMonth);
        int totalEvents = monthEvents.size();

        Set<UUID> eventIds = monthEvents.stream().map(Event::getId).collect(Collectors.toSet());
        long uniqueParticipants = eventIds.isEmpty() ? 0 :
                participantEventRepository.countUniqueParticipantsByEventIds(eventIds);

        Map<EventType, Long> eventsByType = monthEvents.stream()
                .collect(Collectors.groupingBy(Event::getType, Collectors.counting()));

        Map<EventStatus, Long> eventsByStatus = monthEvents.stream()
                .collect(Collectors.groupingBy(Event::getStatus, Collectors.counting()));

        log.info("✅ Rapport mensuel : {} événements, {} participants uniques", totalEvents, uniqueParticipants);

        return MonthlyReportDto.builder()
                .month(month)
                .year(year)
                .totalEvents(totalEvents)
                .totalParticipants((int) uniqueParticipants)
                .eventsByType(eventsByType)
                .eventsByStatus(eventsByStatus)
                .events(eventMapper.toDtos(monthEvents))
                .build();
    }

    @Override
    public Map<String, Map<String, Long>> getEventsByStatusAndMonth(int year) {
        log.info("📆 Chargement des événements par statut pour l'année {}", year);

        List<Event> events = eventRepository.findByYear(year);
        String[] months = {"Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
                "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};

        Map<String, Map<String, Long>> result = new LinkedHashMap<>();
        for (EventStatus status : EventStatus.values()) {
            Map<String, Long> monthMap = Arrays.stream(months)
                    .collect(Collectors.toMap(m -> m, m -> 0L, (a, b) -> a, LinkedHashMap::new));
            result.put(status.name(), monthMap);
        }

        for (Event event : events) {
            if (event.getStartDate() == null) continue;
            int monthIndex = event.getStartDate().getMonthValue() - 1;
            if (monthIndex < 0 || monthIndex >= 12) continue;
            String monthName = months[monthIndex];
            String statusName = event.getStatus().name();
            result.get(statusName).merge(monthName, 1L, Long::sum);
        }

        log.info("✅ Données mensuelles prêtes pour l'année {}", year);
        return result;
    }
}