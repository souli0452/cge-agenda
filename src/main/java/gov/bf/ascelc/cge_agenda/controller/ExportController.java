package gov.bf.ascelc.cge_agenda.controller;

import gov.bf.ascelc.cge_agenda.dto.EventDto;
import gov.bf.ascelc.cge_agenda.enums.EventStatus;
import gov.bf.ascelc.cge_agenda.enums.EventType;
import gov.bf.ascelc.cge_agenda.service.EventService;
import gov.bf.ascelc.cge_agenda.service.PdfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/cge-agenda/events/export")
@RequiredArgsConstructor
@Slf4j
public class ExportController {

    private final PdfService pdfService;
    private final EventService eventService;

    /**
     * ✅ Export PDF mensuel
     * GET /api/v1/cge-agenda/events/export/monthly?year=2026&month=1
     */
    @GetMapping("/monthly")
    public ResponseEntity<byte[]> exportMonthly(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) EventType type,
            @RequestParam(required = false) EventStatus status
    ) {
        log.info("📥 Export mensuel: {}/{} (filtres: keyword={}, type={}, status={})",
                month, year, keyword, type, status);

        List<EventDto> events = eventService.getEventsByMonth(year, month);
        events = applyFilters(events, keyword, type, status);

        byte[] pdf = pdfService.generateEventsReportPDF(events);

        String monthName = getMonthName(month);
        String filename = String.format("evenements_%s_%d.pdf", monthName, year);
        return buildResponse(pdf, filename);
    }

    // ==========================================
    // MÉTHODE DE FILTRAGE
    // ==========================================
    private List<EventDto> applyFilters(List<EventDto> events, String keyword,
                                        EventType type, EventStatus status) {
        return events.stream()
                .filter(event -> {
                    boolean matchesKeyword = keyword == null ||
                            event.getTitle().toLowerCase().contains(keyword.toLowerCase()) ||
                            (event.getDescription() != null &&
                                    event.getDescription().toLowerCase().contains(keyword.toLowerCase()));

                    boolean matchesType = type == null || event.getType() == type;
                    boolean matchesStatus = status == null || event.getStatus() == status;

                    return matchesKeyword && matchesType && matchesStatus;
                })
                .toList();
    }

    // ==========================================
    // MÉTHODES UTILITAIRES
    // ==========================================
    private ResponseEntity<byte[]> buildResponse(byte[] pdfContent, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setContentLength(pdfContent.length);

        log.info("✅ PDF généré: {} ({} bytes)", filename, pdfContent.length);
        return ResponseEntity.ok().headers(headers).body(pdfContent);
    }

    private String getMonthName(int month) {
        String[] months = {
                "janvier", "fevrier", "mars", "avril", "mai", "juin",
                "juillet", "aout", "septembre", "octobre", "novembre", "decembre"
        };
        return months[month - 1];
    }
}