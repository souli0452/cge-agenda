
package gov.bf.ascelc.cge_agenda.controller;

import gov.bf.ascelc.cge_agenda.dto.EventDto;
import gov.bf.ascelc.cge_agenda.dto.ParticipantDto;
import gov.bf.ascelc.cge_agenda.entities.Event;
import gov.bf.ascelc.cge_agenda.enums.EventStatus;
import gov.bf.ascelc.cge_agenda.enums.EventType;
import gov.bf.ascelc.cge_agenda.repository.EventRepository;
import gov.bf.ascelc.cge_agenda.service.EmailService;
import gov.bf.ascelc.cge_agenda.service.EventService;
import gov.bf.ascelc.cge_agenda.service.PdfService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static gov.bf.ascelc.cge_agenda.utils.ApiUrls.*;

@RestController
@RequestMapping(EVENT_ROOT_URL)
@RequiredArgsConstructor
@Slf4j
public class EventController {

    private final EventService eventService;
    private final EventRepository eventRepository;
    private final EmailService emailService;
    private final PdfService pdfService;

    // ==========================================
    // GESTION DES ÉVÉNEMENTS
    // ==========================================

    /**
     * Créer un événement (simple, multi-jours, ou avec horaires multiples)
     */
    @PostMapping(CREATE_EVENT)
    public ResponseEntity<EventDto> create(@Valid @RequestBody EventDto eventDto) {
        EventDto event = eventService.create(eventDto);
        return new ResponseEntity<>(event, HttpStatus.CREATED);
    }

    /**
     * Mettre à jour un événement
     */
    @PutMapping(UPDATE_EVENT)
    public ResponseEntity<EventDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody EventDto eventDto
    ) {
        EventDto updatedEvent = eventService.update(id, eventDto);

        // Envoyer notification aux participants
        try {
            Event event = eventRepository.findById(id).orElseThrow();
            emailService.sendEventUpdateNotification(event);
        } catch (Exception e) {
            // Ne pas bloquer si l'envoi échoue
            System.err.println("Erreur notification : " + e.getMessage());
        }

        return ResponseEntity.ok(updatedEvent);
    }

    /**
     * Annuler un événement
     */
    @PatchMapping(CANCEL_EVENT)
    public ResponseEntity<EventDto> cancelEvent(
            @PathVariable UUID id,
            @RequestParam(required = false, defaultValue = "Non spécifiée") String reason
    ) {
        EventDto event = eventService.cancelEvent(id, reason);
        return ResponseEntity.ok(event);
    }

    /**
     * Reporter un événement
     */
    @PatchMapping(POSTPONE_EVENT)
    public ResponseEntity<EventDto> postponeEvent(
            @PathVariable UUID id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newStartDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate newEndDate
    ) {
        EventDto event = eventService.postponeEvent(id, newStartDate, newEndDate);
        return ResponseEntity.ok(event);
    }

    /**
     * Supprimer un événement
     */
    @DeleteMapping(DELETE_EVENT)
    public ResponseEntity<Void> deleteById(@PathVariable UUID id) {
        eventService.delete(id);
        return ResponseEntity.noContent().build();
    }

    // ==========================================
    // CONSULTATION D'ÉVÉNEMENTS
    // ==========================================

    /**
     * Lister tous les événements
     */
    @GetMapping(GET_ALL_EVENT)
    public ResponseEntity<List<EventDto>> allEvents() {
        List<EventDto> events = eventService.allEvents();
        return ResponseEntity.ok(events);
    }

    /**
     * Voir les détails d'un événement
     */
    @GetMapping(GET_EVENT_BY_ID)
    public ResponseEntity<EventDto> getEventById(@PathVariable UUID id) {
        EventDto event = eventService.getEventById(id);
        return ResponseEntity.ok(event);
    }

    /**
     * Rechercher des événements par critères
     */
    @GetMapping(SEARCH_EVENTS)
    public ResponseEntity<List<EventDto>> searchEvents(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) EventType type,
            @RequestParam(required = false) EventStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<EventDto> events = eventService.searchEvents(keyword, type, status, startDate, endDate);
        return ResponseEntity.ok(events);
    }

    /**
     * Consulter le calendrier mensuel
     */
    @GetMapping(CALENDAR_MONTHLY)
    public ResponseEntity<List<EventDto>> getEventsByMonth(
            @PathVariable int year,
            @PathVariable int month
    ) {
        List<EventDto> events = eventService.getEventsByMonth(year, month);
        return ResponseEntity.ok(events);
    }

    /**
     * Récupérer les événements d'une période
     */
    @GetMapping(EVENTS_BY_PERIOD)
    public ResponseEntity<List<EventDto>> getEventsByPeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        List<EventDto> events = eventService.getEventsByDateRange(startDate, endDate);
        return ResponseEntity.ok(events);
    }

    // ==========================================
    // GESTION DES PARTICIPANTS
    // ==========================================

    /**
     * Ajouter un participant à un événement
     */
    @PostMapping(ADD_PARTICIPANT_TO_EVENT)
    public ResponseEntity<EventDto> addParticipant(
            @PathVariable UUID eventId,
            @Valid @RequestBody ParticipantDto participantDto
    ) {
        EventDto event = eventService.addParticipant(eventId, participantDto);
        return ResponseEntity.ok(event);
    }

    /**
     * Retirer un participant d'un événement
     */
    @DeleteMapping(REMOVE_PARTICIPANT_FROM_EVENT)
    public ResponseEntity<EventDto> removeParticipant(
            @PathVariable UUID eventId,
            @PathVariable UUID participantId
    ) {
        EventDto event = eventService.removeParticipant(eventId, participantId);
        return ResponseEntity.ok(event);
    }

    /**
     * Consulter la liste des participants d'un événement
     */
    @GetMapping(GET_EVENT_PARTICIPANTS)
    public ResponseEntity<List<ParticipantDto>> getEventParticipants(@PathVariable UUID eventId) {
        List<ParticipantDto> participants = eventService.getEventParticipants(eventId);
        return ResponseEntity.ok(participants);
    }

    /**
     * Importer une liste de participants (CSV/Excel)
     */
    @PostMapping(IMPORT_PARTICIPANTS)
    public ResponseEntity<List<ParticipantDto>> importParticipants(
            @PathVariable UUID eventId,
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        String filename = file.getOriginalFilename();
        String fileExtension = filename != null ?
                filename.substring(filename.lastIndexOf(".") + 1).toLowerCase() : "";

        if (!fileExtension.equals("csv") && !fileExtension.equals("xlsx") &&
                !fileExtension.equals("xls")) {
            return ResponseEntity.badRequest().build();
        }

        List<ParticipantDto> participants = eventService.importParticipants(
                eventId,
                file.getBytes(),
                fileExtension
        );

        return ResponseEntity.ok(participants);
    }

    /**
     * Génère la liste d'émargement Excel pour un événement
     *
     * @param id L'UUID de l'événement
     * @return Fichier Excel téléchargeable
     */
    @GetMapping(GENERATE_ATTENDANCE_SHEET)
    public ResponseEntity<byte[]> generateAttendanceSheet(@PathVariable UUID id) {
        log.info("Génération de la liste d'émargement pour l'événement : {}", id);

        byte[] excelFile = eventService.generateAttendanceSheet(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDispositionFormData("attachment",
                "liste_emargement_" + id + ".xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .body(excelFile);
    }

    /**
     * Génère le calendrier PDF d'un mois
     */
    @GetMapping("/calendar/pdf/{year}/{month}")
    public ResponseEntity<byte[]> generateCalendarPDF(
            @PathVariable int year,
            @PathVariable int month
    ) {
        log.info("Génération du calendrier PDF pour {}/{}", month, year);

        byte[] pdfFile = pdfService.generateCalendarPDF(year, month);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment",
                "calendrier_" + year + "_" + month + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfFile);
    }

    /**
     * Génère le compte-rendu PDF d'un événement
     */
    @GetMapping("/compte-rendu/{id}")
    public ResponseEntity<byte[]> generateMeetingReport(@PathVariable UUID id) {
        log.info("Génération du compte-rendu pour l'événement : {}", id);

        byte[] pdfFile = pdfService.generateMeetingReportPDF(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment",
                "compte_rendu_" + id + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfFile);
    }

    @PostMapping("/test-email/{eventId}")
    public ResponseEntity<String> testEmail(@PathVariable UUID eventId) {
        Event event = eventRepository.findById(eventId).orElseThrow();
        emailService.sendEventReminder(event, 7);
        return ResponseEntity.ok("Email envoyé !");
    }
}