package gov.bf.ascelc.cge_agenda.utils;

import gov.bf.ascelc.cge_agenda.dto.EventDto;
import gov.bf.ascelc.cge_agenda.dto.ScheduleDto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

/**
 * Génère un fichier .ics (RFC 5545) pour un événement, afin qu'un utilisateur puisse
 * l'ajouter à son calendrier personnel (Outlook, Google Calendar, etc.). Le Burkina Faso
 * n'a pas d'heure d'été et est en UTC+00:00 (Africa/Ouagadougou) : l'heure locale de
 * l'application EST l'heure UTC, d'où le suffixe "Z" direct sans conversion de fuseau.
 */
public final class ICalendarUtils {

    private ICalendarUtils() {}

    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");

    public static String buildIcs(EventDto event, String baseUrl) {
        LocalDateTime[] bounds = resolveBounds(event);
        LocalDateTime start = bounds[0];
        LocalDateTime end = bounds[1];

        StringBuilder sb = new StringBuilder();
        line(sb, "BEGIN:VCALENDAR");
        line(sb, "VERSION:2.0");
        line(sb, "PRODID:-//ASCE-LC//CGE Agenda//FR");
        line(sb, "CALSCALE:GREGORIAN");
        line(sb, "BEGIN:VEVENT");
        line(sb, "UID:" + event.getId() + "@agenda.asce-lc.bf");
        line(sb, "DTSTAMP:" + LocalDateTime.now().format(DATE_TIME_FMT));
        line(sb, "DTSTART:" + start.format(DATE_TIME_FMT));
        line(sb, "DTEND:" + end.format(DATE_TIME_FMT));
        line(sb, "SUMMARY:" + escape(event.getTitle()));
        if (event.getDescription() != null && !event.getDescription().isBlank()) {
            line(sb, "DESCRIPTION:" + escape(event.getDescription()));
        }
        String location = buildLocation(event);
        if (location != null && !location.isBlank()) {
            line(sb, "LOCATION:" + escape(location));
        }
        if (baseUrl != null) {
            line(sb, "URL:" + baseUrl + "/events/" + event.getId());
        }
        line(sb, "END:VEVENT");
        line(sb, "END:VCALENDAR");
        return sb.toString();
    }

    private static LocalDateTime[] resolveBounds(EventDto event) {
        LocalDate startDate = event.getStartDate();
        LocalDate endDate = event.getEndDate() != null ? event.getEndDate() : startDate;
        LocalTime startTime = event.getGlobalStartTime();
        LocalTime endTime = event.getGlobalEndTime();

        List<ScheduleDto> schedules = event.getSchedules();
        if ((startTime == null || endTime == null) && schedules != null && !schedules.isEmpty()) {
            ScheduleDto first = schedules.stream()
                    .min(Comparator.comparing(ScheduleDto::getDateJour).thenComparing(ScheduleDto::getStartTime))
                    .orElse(null);
            ScheduleDto last = schedules.stream()
                    .max(Comparator.comparing(ScheduleDto::getDateJour).thenComparing(ScheduleDto::getEndTime))
                    .orElse(null);
            if (first != null) {
                startDate = first.getDateJour();
                startTime = first.getStartTime();
            }
            if (last != null) {
                endDate = last.getDateJour();
                endTime = last.getEndTime();
            }
        }

        if (startTime == null) startTime = LocalTime.of(8, 0);
        if (endTime == null) endTime = LocalTime.of(18, 0);

        return new LocalDateTime[]{ startDate.atTime(startTime), endDate.atTime(endTime) };
    }

    private static String buildLocation(EventDto event) {
        if ("INTERNE".equals(event.getLieuType())) {
            return event.getSalle() != null ? "ASCELC — " + event.getSalle() : "ASCELC";
        }
        StringBuilder loc = new StringBuilder();
        if (event.getNomLieu() != null && !event.getNomLieu().isBlank()) loc.append(event.getNomLieu());
        if (event.getVille() != null && !event.getVille().isBlank()) {
            if (loc.length() > 0) loc.append(", ");
            loc.append(event.getVille());
        }
        if (event.getPays() != null && !event.getPays().isBlank()) {
            if (loc.length() > 0) loc.append(", ");
            loc.append(event.getPays());
        }
        return loc.toString();
    }

    /** Échappement RFC 5545 des valeurs texte : virgule, point-virgule, antislash, retour ligne. */
    private static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace(";", "\\;")
                .replace(",", "\\,")
                .replace("\n", "\\n");
    }

    /** Repli de ligne (folding) à 75 octets, imposé par la RFC pour la compatibilité large. */
    private static void line(StringBuilder sb, String content) {
        int max = 75;
        int i = 0;
        boolean first = true;
        while (i < content.length()) {
            int end = Math.min(i + (first ? max : max - 1), content.length());
            if (!first) sb.append(' ');
            sb.append(content, i, end);
            sb.append("\r\n");
            i = end;
            first = false;
        }
        if (content.isEmpty()) {
            sb.append("\r\n");
        }
    }
}
