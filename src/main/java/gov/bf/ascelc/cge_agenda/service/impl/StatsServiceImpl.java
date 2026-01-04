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
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        log.info("Génération des statistiques du dashboard");

        // Récupérer tous les événements
        List<Event> allEvents = eventRepository.findAll();

        // Total événements
        long totalEvents = allEvents.size();

        // Total participants (unique)
        int totalParticipants = (int) participantEventRepository.count();

        // Grouper par statut
        Map<EventStatus, Long> eventsByStatus = allEvents.stream()
                .collect(Collectors.groupingBy(Event::getStatus, Collectors.counting()));

        // Grouper par type
        Map<EventType, Long> eventsByType = allEvents.stream()
                .collect(Collectors.groupingBy(Event::getType, Collectors.counting()));

        // Événements à venir (7 prochains jours)
        LocalDate today = LocalDate.now();
        LocalDate weekFromNow = today.plusDays(7);

        List<Event> upcomingEvents = allEvents.stream()
                .filter(e -> e.getStartDate().isAfter(today) &&
                        e.getStartDate().isBefore(weekFromNow))
                .sorted(Comparator.comparing(Event::getStartDate))
                .toList();

        // Top 3 types d'événements
        List<TypeStatsDto> topTypes = eventsByType.entrySet().stream()
                .sorted(Map.Entry.<EventType, Long>comparingByValue().reversed())
                .limit(3)
                .map(entry -> TypeStatsDto.builder()
                        .type(entry.getKey())
                        .count(entry.getValue())
                        .percentage((double) entry.getValue() / totalEvents * 100)
                        .build())
                .toList();

        log.info("Statistiques générées : {} événements, {} participants",
                totalEvents, totalParticipants);

        return DashboardStatsDto.builder()
                .totalEvents(totalEvents)
                .totalParticipants(totalParticipants)
                .upcomingEventsCount(upcomingEvents.size())
                .eventsByStatus(eventsByStatus)
                .eventsByType(eventsByType)
                .upcomingEvents(eventMapper.toDtos(upcomingEvents))
                .topEventTypes(topTypes)
                .build();
    }

    @Override
    public MonthlyReportDto getMonthlyReport(int year, int month) {
        log.info("Génération du rapport mensuel : {}/{}", month, year);

        // Calculer les dates de début et fin du mois
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        // Récupérer les événements du mois
        List<Event> monthEvents = eventRepository.findAll().stream()
                .filter(e -> !e.getStartDate().isBefore(startDate) &&
                        !e.getStartDate().isAfter(endDate))
                .toList();

        // Total événements
        int totalEvents = monthEvents.size();

        // Total participants du mois
        int totalParticipants = monthEvents.stream()
                .mapToInt(e -> e.getParticipantEvents().size())
                .sum();

        // Grouper par type
        Map<EventType, Long> eventsByType = monthEvents.stream()
                .collect(Collectors.groupingBy(Event::getType, Collectors.counting()));

        // Grouper par statut
        Map<EventStatus, Long> eventsByStatus = monthEvents.stream()
                .collect(Collectors.groupingBy(Event::getStatus, Collectors.counting()));

        log.info("Rapport mensuel généré : {} événements, {} participants",
                totalEvents, totalParticipants);

        return MonthlyReportDto.builder()
                .month(month)
                .year(year)
                .totalEvents(totalEvents)
                .totalParticipants(totalParticipants)
                .eventsByType(eventsByType)
                .eventsByStatus(eventsByStatus)
                .events(eventMapper.toDtos(monthEvents))
                .build();
    }


    @Override
    public Map<String, Map<String, Long>> getEventsByStatusAndMonth(int year) {
        log.info("=== DÉBUT getEventsByStatusAndMonth pour l'année : {} ===", year);

        // Récupérer TOUS les événements
        List<Event> allEventsBeforeFilter = eventRepository.findAll();
        log.info("Total événements dans la base : {}", allEventsBeforeFilter.size());

        // Afficher les années des événements
        allEventsBeforeFilter.forEach(e -> {
            log.info("Événement '{}' : startDate={}, année={}, statut={}",
                    e.getTitle(),
                    e.getStartDate(),
                    e.getStartDate() != null ? e.getStartDate().getYear() : "NULL",
                    e.getStatus());
        });

        // Filtrer par année
        List<Event> allEvents = allEventsBeforeFilter.stream()
                .filter(e -> e.getStartDate() != null && e.getStartDate().getYear() == year)
                .toList();

        log.info("Événements après filtre année {} : {}", year, allEvents.size());

        String[] months = {"Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
                "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"};

        Map<String, Map<String, Long>> result = new LinkedHashMap<>();

        // Initialiser pour chaque statut
        for (EventStatus status : EventStatus.values()) {
            Map<String, Long> monthsMap = new LinkedHashMap<>();
            for (String month : months) {
                monthsMap.put(month, 0L);
            }
            result.put(status.name(), monthsMap);
        }

        // Compter les événements
        for (Event event : allEvents) {
            int monthIndex = event.getStartDate().getMonthValue() - 1;
            String monthName = months[monthIndex];
            String statusName = event.getStatus().name();

            Map<String, Long> statusMonths = result.get(statusName);
            statusMonths.put(monthName, statusMonths.get(monthName) + 1);

            log.info("✓ Comptage : Événement '{}' → {} / {}",
                    event.getTitle(), statusName, monthName);
        }

        log.info("=== FIN getEventsByStatusAndMonth ===");
        return result;
    }


}