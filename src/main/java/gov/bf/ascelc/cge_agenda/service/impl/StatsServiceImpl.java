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
import gov.bf.ascelc.cge_agenda.service.EspaceService;
import gov.bf.ascelc.cge_agenda.service.StatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
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
    private final EspaceService espaceService;

    /**
     * Espaces accessibles à l'utilisateur courant (null = ADMIN, vue transverse). Les
     * statistiques d'un chef ne portent que sur son propre espace — jamais mélangées
     * avec celles des autres chefs.
     */
    private List<UUID> espacesAccessiblesCourantOuNull() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (isAdmin) {
            return null;
        }
        String email = null;
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            email = jwt.getClaim("email");
            if (email == null) {
                email = jwt.getClaim("preferred_username");
            }
        }
        return espaceService.espacesAccessibles(email);
    }

    private boolean accessible(Event event, List<UUID> espaceIds) {
        if (espaceIds == null) {
            return true;
        }
        return event.getEspace() != null && espaceIds.contains(event.getEspace().getId());
    }

    private List<Event> filterAccessible(List<Event> events, List<UUID> espaceIds) {
        if (espaceIds == null) {
            return events;
        }
        return events.stream().filter(e -> accessible(e, espaceIds)).toList();
    }

    @Override
    public DashboardStatsDto getDashboardStats() {
        log.info("📊 Génération des statistiques du dashboard");

        List<UUID> espaceIds = espacesAccessiblesCourantOuNull();
        List<Event> allEvents = filterAccessible(eventRepository.findAll(), espaceIds);
        long totalEvents = allEvents.size();

        // Événements à venir (7 prochains jours)
        LocalDate today = LocalDate.now();
        LocalDate weekFromNow = today.plusDays(7);
        List<Event> upcomingEvents = filterAccessible(
                eventRepository.findUpcomingEvents(today, weekFromNow), espaceIds);

        // Agrégations par statut et par type
        Map<EventStatus, Long> eventsByStatus = allEvents.stream()
                .collect(Collectors.groupingBy(Event::getStatus, Collectors.counting()));
        Map<EventType, Long> eventsByType = allEvents.stream()
                .collect(Collectors.groupingBy(Event::getType, Collectors.counting()));

        // Participants/inscriptions scopés aux mêmes événements accessibles
        long uniqueParticipants;
        long totalInscriptions;
        if (espaceIds == null) {
            uniqueParticipants = participantEventRepository.countUniqueParticipants();
            totalInscriptions = participantEventRepository.count();
        } else {
            List<UUID> eventIds = allEvents.stream().map(Event::getId).toList();
            uniqueParticipants = eventIds.isEmpty() ? 0 : participantEventRepository.countUniqueParticipantsByEventIds(eventIds);
            totalInscriptions = eventIds.isEmpty() ? 0 : participantEventRepository.countByEventIdIn(eventIds);
        }

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

        List<Event> monthEvents = filterAccessible(
                eventRepository.findByMonth(startOfMonth, endOfMonth), espacesAccessiblesCourantOuNull());
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

        List<Event> events = filterAccessible(
                eventRepository.findByYear(year), espacesAccessiblesCourantOuNull());
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
