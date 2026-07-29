package gov.bf.ascelc.cge_agenda.service.impl;

import gov.bf.ascelc.cge_agenda.dto.*;
import gov.bf.ascelc.cge_agenda.entities.*;
import gov.bf.ascelc.cge_agenda.enums.EventStatus;
import gov.bf.ascelc.cge_agenda.enums.EventType;
import gov.bf.ascelc.cge_agenda.mapper.EventMapper;
import gov.bf.ascelc.cge_agenda.mapper.ParticipantMapper;
import gov.bf.ascelc.cge_agenda.repository.EventRepository;
import gov.bf.ascelc.cge_agenda.repository.ParticipantEventRepository;
import gov.bf.ascelc.cge_agenda.repository.ParticipantRepository;
import gov.bf.ascelc.cge_agenda.repository.ScheduleRepository;
import gov.bf.ascelc.cge_agenda.service.EmailService;
import gov.bf.ascelc.cge_agenda.service.EventService;
import gov.bf.ascelc.cge_agenda.utils.ValidationUtils;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;

import org.apache.poi.ss.usermodel.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class EventServiceImpl implements EventService {

    private final EventRepository eventRepository;
    private final EventMapper eventMapper;
    private final ScheduleRepository scheduleRepository;
    private final ParticipantRepository participantRepository;
    private final ParticipantEventRepository participantEventRepository;
    private final ParticipantMapper participantMapper;
    private final EmailService emailService;


    @Override
    @Transactional(rollbackFor = Exception.class)
    public EventDto create(EventDto dto) {
        log.info("=== DEBUT CREATE === title='{}' type={} start={} end={}",
                dto.getTitle(), dto.getType(), dto.getStartDate(), dto.getEndDate());
        log.info("globalStart={} globalEnd={} schedules.size={}",
                dto.getGlobalStartTime(), dto.getGlobalEndTime(),
                dto.getSchedules() != null ? dto.getSchedules().size() : "null");

        validateEventDates(dto.getStartDate(), dto.getEndDate());
        log.info("validateEventDates OK");

        validateScheduleMode(dto);
        log.info("validateScheduleMode OK");

        Event event = eventMapper.toEntity(dto);
        event.setCreatedAt(LocalDateTime.now());
        event.setCreatorEmail(currentUserEmail());
        event.setCreatorUsername(currentUsername());
        event.setCreatorRole(currentUserRole());
        event = eventRepository.save(event);
        log.info("event sauvegardé ID={}", event.getId());

        // Fichiers
        if (dto.getFiles() != null && !dto.getFiles().isEmpty()) {
            List<File> savedFiles = new ArrayList<>();
            for (FileDto fileDto : dto.getFiles()) {
                File file = eventMapper.toFileEntity(fileDto);
                file.setEvent(event);
                file.setCreatedAt(LocalDateTime.now());
                savedFiles.add(file);
            }
            event.setFiles(new HashSet<>(savedFiles));
            log.info("{} fichiers attachés", savedFiles.size());
        }

        // Schedules
        List<Schedule> schedules = createSchedules(event, dto);
        log.info("{} schedules créés", schedules.size());

        // Participants
        List<UUID[]> participantInvitations = new ArrayList<>();
        if (dto.getParticipants() != null && !dto.getParticipants().isEmpty()) {
            log.info("Traitement de {} participants...",
                    dto.getParticipants().size());
            participantInvitations = processParticipants(
                    event, dto.getParticipants(), schedules);
            log.info("{} participants traités",
                    participantInvitations.size());
        }

        event = eventRepository.save(event);
        log.info("event final sauvegardé");

        final List<UUID[]> finalInvitations = participantInvitations;
        if (!finalInvitations.isEmpty()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            log.info("Transaction commitée → {} invitation(s)",
                                    finalInvitations.size());
                            for (UUID[] ids : finalInvitations) {
                                emailService.sendEventInvitation(ids[0], ids[1]);
                            }
                        }
                    }
            );
        }

        log.info(" CREATE TERMINÉ");
        return enrichWithStructures(eventMapper.toDto(event), event);
    }

    // ==========================================
    // ANNULER UN ÉVÉNEMENT
    // ==========================================
    @Override
    public EventDto cancelEvent(UUID id, String reason) {
        log.info("Annulation de l'événement : ID = {}", id);
        return eventRepository.findById(id)
                .map(event -> {
                    event.setStatus(EventStatus.ANNULER);
                    event.setDescription(event.getDescription() +
                            "\n\n[ANNULÉ] Raison : " + reason);
                    event.setUpdatedAt(LocalDateTime.now());
                    Event updated = eventRepository.save(event);
                    final UUID uid = updated.getId();

                    TransactionSynchronizationManager.registerSynchronization(
                            new TransactionSynchronization() {
                                @Override
                                public void afterCommit() {
                                    emailService.sendEventCancellation(uid, reason);
                                }
                            });

                    log.info("Événement annulé : ID = {}", id);
                    return eventMapper.toDto(updated);
                })
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Événement non trouvé avec l'ID : " + id));
    }

    // ==========================================
    // REPORTER UN ÉVÉNEMENT
    // ==========================================
    @Override
    public EventDto postponeEvent(UUID id, LocalDate newStartDate, LocalDate newEndDate) {
        log.info("Report de l'événement : ID = {}, {} - {}",
                id, newStartDate, newEndDate);
        validateEventDates(newStartDate, newEndDate);

        return eventRepository.findById(id)
                .map(event -> {
                    LocalTime globalStartTime = null;
                    LocalTime globalEndTime   = null;

                    if (!event.getSchedules().isEmpty()) {
                        Schedule first = event.getSchedules().stream()
                                .findFirst().orElse(null);
                        if (first != null) {
                            globalStartTime = first.getStartTime();
                            globalEndTime   = first.getEndTime();
                        }
                    }

                    event.setStatus(EventStatus.REPORTER);
                    event.setStartDate(newStartDate);
                    event.setEndDate(newEndDate);
                    event.setUpdatedAt(LocalDateTime.now());
                    event.getSchedules().clear();

                    Event updated = eventRepository.save(event);
                    scheduleRepository.deleteByEventId(id);

                    if (globalStartTime != null && globalEndTime != null) {
                        LocalDate cur = newStartDate;
                        while (!cur.isAfter(newEndDate)) {
                            scheduleRepository.save(Schedule.builder()
                                    .dateJour(cur)
                                    .startTime(globalStartTime)
                                    .endTime(globalEndTime)
                                    .event(updated)
                                    .createdAt(LocalDateTime.now())
                                    .build());
                            cur = cur.plusDays(1);
                        }
                        log.info("Horaires recréés pour les nouvelles dates");
                    }

                    final UUID uid = updated.getId();
                    TransactionSynchronizationManager.registerSynchronization(
                            new TransactionSynchronization() {
                                @Override
                                public void afterCommit() {
                                    emailService.sendEventPostponement(uid);
                                }
                            });

                    log.info("Événement reporté : ID = {}", id);
                    return eventMapper.toDto(updated);
                })
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Événement non trouvé avec l'ID : " + id));
    }

    // ==========================================
    // RECHERCHE D'ÉVÉNEMENTS
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public List<EventDto> searchEvents(String keyword, EventType type,
                                       EventStatus status,
                                       LocalDate startDate, LocalDate endDate) {
        log.info("Recherche : keyword={}, type={}, status={}",
                keyword, type, status);
        List<Event> events = eventRepository.findAll().stream()
                .filter(e -> !e.isDeleted())
                .collect(Collectors.toList());

        if (keyword != null && !keyword.trim().isEmpty()) {
            String lk = keyword.toLowerCase();
            events = events.stream()
                    .filter(e -> e.getTitle().toLowerCase().contains(lk) ||
                            (e.getDescription() != null &&
                                    e.getDescription().toLowerCase().contains(lk)))
                    .collect(Collectors.toList());
        }
        if (type   != null) events = events.stream()
                .filter(e -> e.getType()   == type)
                .collect(Collectors.toList());
        if (status != null) events = events.stream()
                .filter(e -> e.getStatus() == status)
                .collect(Collectors.toList());
        if (startDate != null && endDate != null) events = events.stream()
                .filter(e -> !e.getEndDate().isBefore(startDate) &&
                        !e.getStartDate().isAfter(endDate))
                .collect(Collectors.toList());

        log.info("{} événements trouvés", events.size());
        return eventMapper.toDtos(events);
    }

    // ==========================================
    // ÉVÉNEMENTS PAR MOIS / PÉRIODE
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public List<EventDto> getEventsByMonth(int year, int month) {
        YearMonth ym = YearMonth.of(year, month);
        return getEventsByDateRange(ym.atDay(1), ym.atEndOfMonth());
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventDto> getEventsByDateRange(LocalDate startDate, LocalDate endDate) {
        log.info("Événements entre {} et {}", startDate, endDate);
        List<Event> events = eventRepository.findAll().stream()
                .filter(e -> !e.getEndDate().isBefore(startDate) &&
                        !e.getStartDate().isAfter(endDate))
                .collect(Collectors.toList());
        log.info("{} événements trouvés", events.size());
        return events.stream()
                .map(e -> enrichWithStructures(eventMapper.toDto(e), e))
                .toList();
    }

    // ==========================================
    // AJOUTER UN PARTICIPANT
    // ==========================================
    @Override
    @Transactional
    public EventDto addParticipant(UUID eventId, ParticipantDto participantDto) {
        log.info("Ajout participant à l'événement : {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Événement non trouvé : " + eventId));

        List<Schedule> schedules = scheduleRepository.findByEventId(eventId);
        Participant participant;

        if (participantDto.getId() != null) {
            participant = participantRepository.findById(participantDto.getId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Participant non trouvé : " + participantDto.getId()));
        } else {
            participant = findOrCreateParticipant(participantDto);
        }

        validateParticipantAvailability(participant, schedules);

        if (participantEventRepository.existsByParticipantIdAndEventId(
                participant.getId(), eventId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    String.format("Le participant %s %s est déjà inscrit",
                            participant.getFirstName(), participant.getLastName()));
        }

        participantEventRepository.save(ParticipantEvent.builder()
                .participant(participant)
                .event(event)
                .createdAt(LocalDateTime.now())
                .build());

        final UUID pId = participant.getId();
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        emailService.sendEventInvitation(eventId, pId);
                    }
                });

        log.info("Participant ajouté : {} {}",
                participant.getFirstName(), participant.getLastName());
        return getEventById(eventId);
    }

    // ==========================================
    // RETIRER UN PARTICIPANT
    // ==========================================
    @Override
    @Transactional
    public EventDto removeParticipant(UUID eventId, UUID participantId) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Événement non trouvé : " + eventId);
        }
        participantEventRepository.deleteByEventIdAndParticipantId(
                eventId, participantId);
        log.info("Participant retiré");
        return getEventById(eventId);
    }

    // ==========================================
    // RÉCUPÉRER LES PARTICIPANTS
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public List<ParticipantDto> getEventParticipants(UUID eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Événement non trouvé : " + eventId);
        }
        return participantMapper.toDtos(
                participantEventRepository.findParticipantsByEventId(eventId));
    }

    // ==========================================
    // IMPORTER DES PARTICIPANTS
    // ==========================================
    @Override
    public List<ParticipantDto> importParticipants(UUID eventId,
                                                   byte[] fileContent,
                                                   String fileType) {
        log.info("Import participants événement={} type={}", eventId, fileType);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Événement non trouvé : " + eventId));

        List<Schedule> schedules = scheduleRepository.findByEventId(eventId);
        List<ParticipantDto> imported = new ArrayList<>();
        List<UUID[]> invitations     = new ArrayList<>();

        try {
            List<ParticipantDto> toImport;
            if ("csv".equalsIgnoreCase(fileType)) {
                toImport = parseCSV(fileContent);
            } else if ("xlsx".equalsIgnoreCase(fileType) ||
                    "xls".equalsIgnoreCase(fileType)) {
                toImport = parseExcel(fileContent);
            } else {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Format non supporté : " + fileType);
            }

            for (ParticipantDto dto : toImport) {
                try {
                    Participant p = findOrCreateParticipant(dto);
                    validateParticipantAvailability(p, schedules);
                    if (!participantEventRepository.existsByParticipantIdAndEventId(
                            p.getId(), eventId)) {
                        participantEventRepository.save(ParticipantEvent.builder()
                                .participant(p).event(event)
                                .createdAt(LocalDateTime.now()).build());
                        invitations.add(new UUID[]{eventId, p.getId()});
                        imported.add(participantMapper.toDto(p));
                    }
                } catch (Exception e) {
                    log.warn("Erreur import {} : {}", dto.getEmail(), e.getMessage());
                }
            }

            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            for (UUID[] ids : invitations) {
                                emailService.sendEventInvitation(ids[0], ids[1]);
                            }
                        }
                    });

            log.info("{} participants importés", imported.size());
            return imported;

        } catch (Exception e) {
            log.error("Erreur import : {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur import : " + e.getMessage());
        }
    }


    @Override
    public EventDto update(UUID id, EventDto eventDto) {
        return eventRepository.findById(id)
                .map(existing -> {
                    validateEventDates(eventDto.getStartDate(), eventDto.getEndDate());

                    boolean hasChanges =
                            isChanged(existing.getTitle(),       eventDto.getTitle())       ||
                                    isChanged(existing.getDescription(), eventDto.getDescription()) ||
                                    !Objects.equals(existing.getType(),   eventDto.getType())       ||
                                    !Objects.equals(existing.getStatus(), eventDto.getStatus())     ||
                                    !Objects.equals(existing.getStartDate(), eventDto.getStartDate()) ||
                                    !Objects.equals(existing.getEndDate(),   eventDto.getEndDate())   ||
                                    isChanged(existing.getVille(),       eventDto.getVille())       ||
                                    isChanged(existing.getPays(),        eventDto.getPays())        ||
                                    isChanged(existing.getMeetingLink(), eventDto.getMeetingLink());

                    log.info("🔍 update id={} hasChanges={}", id, hasChanges);

                    boolean wasACorriger = existing.getStatus() == EventStatus.A_CORRIGER;

                    eventMapper.updateEntityFromDto(eventDto, existing);
                    existing.setUpdatedAt(LocalDateTime.now());

                    // Re-soumission automatique après correction demandée par le CGE
                    if (wasACorriger && hasChanges) {
                        existing.setStatus(EventStatus.EN_ATTENTE_VALIDATION);
                        existing.setChangeSuggestions(null);
                    }

                    Event updated = eventRepository.save(existing);
                    final UUID uid = updated.getId();

                    if (wasACorriger && hasChanges) {
                        TransactionSynchronizationManager.registerSynchronization(
                                new TransactionSynchronization() {
                                    @Override
                                    public void afterCommit() {
                                        emailService.sendAmendmentsCorrected(uid);
                                    }
                                });
                        log.info("✅ Re-soumission automatique après corrections → {}", uid);
                    } else if (hasChanges) {
                        TransactionSynchronizationManager.registerSynchronization(
                                new TransactionSynchronization() {
                                    @Override
                                    public void afterCommit() {
                                        emailService.sendEventUpdateNotification(uid);
                                    }
                                });
                        log.info("✅ Notification modification → {}", uid);
                    } else {
                        log.info("⏭ Aucun changement → pas de notification");
                    }

                    return eventMapper.toDto(updated);
                })
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Événement non trouvé : " + id));
    }


    private boolean isChanged(String existing, String incoming) {
        String a = (existing == null || existing.equals("null"))
                ? "" : existing.trim();
        String b = (incoming == null || incoming.equals("null"))
                ? "" : incoming.trim();
        return !a.equals(b);
    }

    // ==========================================
    // TEST REMINDER
    // ==========================================
    @Override
    public void sendTestReminder(UUID eventId) {
        if (!eventRepository.existsById(eventId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Événement non trouvé : " + eventId);
        }
        emailService.sendEventReminder(eventId, 7);
    }

    // ==========================================
    // ALL EVENTS
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public List<EventDto> allEvents() {
        try {
            return eventRepository
                    .findAllWithParticipantsOrderedByStatusAndProximity()
                    .stream()
                    .map(e -> enrichWithStructures(eventMapper.toDto(e), e))
                    .toList();
        } catch (Exception e) {
            log.error("Erreur chargement événements", e);
            throw new RuntimeException("Impossible de charger les événements");
        }
    }

    // ==========================================
    // GET BY ID
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public EventDto getEventById(UUID id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Événement non trouvé : " + id));
        return enrichWithStructures(eventMapper.toDto(event), event);
    }

    // ==========================================
    // DELETE
    // ==========================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(UUID id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Événement non trouvé : " + id));

        event.setDeleted(true);
        eventRepository.save(event);
        log.info("✓ Événement mis à la corbeille : {}", id);
    }

    // ==========================================
    // CORBEILLE
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public List<EventDto> getCorbeille() {
        return eventRepository.findAllDeleted().stream()
                .map(e -> enrichWithStructures(eventMapper.toDto(e), e))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EventDto restoreEvent(UUID id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Événement non trouvé : " + id));

        event.setDeleted(false);
        event = eventRepository.save(event);
        log.info("✓ Événement restauré : {}", id);
        return enrichWithStructures(eventMapper.toDto(event), event);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteEventPermanently(UUID id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Événement non trouvé : " + id));

        log.info("Suppression définitive : {} schedules, {} fichiers, {} participants",
                event.getSchedules().size(),
                event.getFiles().size(),
                event.getParticipantEvents().size());

        if (!event.getFiles().isEmpty()) {
            event.getFiles().forEach(file -> {
                try {
                    if (Files.deleteIfExists(Paths.get(file.getFilePath()))) {
                        log.info("✓ Fichier physique supprimé : {}", file.getFileName());
                    }
                } catch (Exception e) {
                    log.error("✗ Erreur suppression {} : {}",
                            file.getFileName(), e.getMessage());
                }
            });
        }

        if (!event.getParticipantEvents().isEmpty())
            participantEventRepository.deleteAll(event.getParticipantEvents());
        if (!event.getSchedules().isEmpty())
            scheduleRepository.deleteAll(event.getSchedules());
        if (!event.getFiles().isEmpty())
            event.getFiles().clear();

        eventRepository.deleteById(id);
        log.info("✓ Événement supprimé définitivement : {}", id);
    }

    // ==========================================
    // WORKFLOW DE VALIDATION CGE
    // ==========================================

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EventDto submitDraft(UUID id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Événement non trouvé : " + id));

        if (event.getStatus() != EventStatus.BROUILLON) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Seul un événement en brouillon peut être soumis à validation");
        }

        event.setStatus(EventStatus.EN_ATTENTE_VALIDATION);
        event.setUpdatedAt(LocalDateTime.now());
        Event saved = eventRepository.save(event);
        final UUID uid = saved.getId();

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        emailService.sendValidationRequest(uid);
                    }
                });

        log.info("✓ Événement soumis à validation : {}", id);
        return eventMapper.toDto(saved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EventDto validateEvent(UUID id, String comment) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Événement non trouvé : " + id));

        if (event.getStatus() != EventStatus.EN_ATTENTE_VALIDATION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Seul un événement en attente de validation peut être validé");
        }

        event.setStatus(EventStatus.PLANIFIE);
        event.setValidationComment(comment);
        event.setUpdatedAt(LocalDateTime.now());
        Event saved = eventRepository.save(event);

        final List<UUID> participantIds = saved.getParticipantEvents().stream()
                .map(pe -> pe.getParticipant().getId())
                .toList();
        final UUID uid = saved.getId();

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        for (UUID pid : participantIds) {
                            emailService.sendEventInvitation(uid, pid);
                        }
                    }
                });

        log.info("✓ Événement validé : {} ({} invitation(s))", id, participantIds.size());
        return eventMapper.toDto(saved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EventDto rejectEvent(UUID id, String reason) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Événement non trouvé : " + id));

        if (event.getStatus() != EventStatus.EN_ATTENTE_VALIDATION
                && event.getStatus() != EventStatus.A_CORRIGER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Seul un événement en attente de validation ou à corriger peut être rejeté");
        }

        event.setStatus(EventStatus.REJETE);
        event.setRejectionReason(reason);
        event.setUpdatedAt(LocalDateTime.now());
        Event saved = eventRepository.save(event);
        final UUID uid = saved.getId();

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        emailService.sendEventRejected(uid);
                    }
                });

        log.info("✓ Événement rejeté : {}", id);
        return eventMapper.toDto(saved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EventDto requestChanges(UUID id, String suggestions) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Événement non trouvé : " + id));

        if (event.getStatus() != EventStatus.EN_ATTENTE_VALIDATION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Seul un événement en attente de validation peut recevoir une demande de modifications");
        }

        event.setStatus(EventStatus.A_CORRIGER);
        event.setChangeSuggestions(suggestions);
        event.setUpdatedAt(LocalDateTime.now());
        Event saved = eventRepository.save(event);
        final UUID uid = saved.getId();

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        emailService.sendChangesRequested(uid);
                    }
                });

        log.info("✓ Modifications demandées : {}", id);
        return eventMapper.toDto(saved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EventDto delegateParticipation(UUID id, String delegueNom, String delegueEmail, String delegueMotif) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Événement non trouvé : " + id));

        if (event.getStatus() != EventStatus.PLANIFIE && event.getStatus() != EventStatus.EN_COURS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La délégation n'est possible que pour un événement planifié ou en cours");
        }

        event.setDelegueNom(delegueNom);
        event.setDelegueEmail(delegueEmail);
        event.setDelegueMotif(delegueMotif);
        event.setEstDelegue(true);
        event.setDelegueDate(LocalDateTime.now());
        event.setDelegueParEmail(currentUserEmail());
        event.setUpdatedAt(LocalDateTime.now());
        Event saved = eventRepository.save(event);
        final UUID uid = saved.getId();

        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        emailService.sendDelegationNotice(uid);
                    }
                });

        log.info("✓ Délégation enregistrée : {} → {}", id, delegueEmail);
        return eventMapper.toDto(saved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EventDto addObservation(UUID id, String observation) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Événement non trouvé : " + id));

        if (event.getStatus() != EventStatus.PLANIFIE && event.getStatus() != EventStatus.EN_COURS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "L'observation n'est possible que pour un événement planifié ou en cours");
        }

        event.setValidationComment(observation);
        event.setUpdatedAt(LocalDateTime.now());
        Event saved = eventRepository.save(event);

        log.info("✓ Observation enregistrée : {}", id);
        return eventMapper.toDto(saved);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public EventDto saveCompteRendu(UUID id, String points, String decisions, String actions) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Événement non trouvé : " + id));

        if (event.getStatus() != EventStatus.TERMINE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le compte-rendu n'est disponible que pour un événement terminé");
        }

        if (!isCgeOrAdmin() && !isCreator(event)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Seuls le CGE, l'administrateur ou le créateur peuvent rédiger le compte-rendu");
        }

        event.setCompteRenduPoints(points);
        event.setCompteRenduDecisions(decisions);
        event.setCompteRenduActions(actions);
        event.setCompteRenduRedigePar(currentUserEmail());
        event.setCompteRenduDate(LocalDateTime.now());
        event.setUpdatedAt(LocalDateTime.now());
        Event saved = eventRepository.save(event);

        log.info("✓ Compte-rendu enregistré : {}", id);
        return eventMapper.toDto(saved);
    }

    private boolean isCgeOrAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_CGE"));
    }

    private boolean isCreator(Event event) {
        String email = currentUserEmail();
        return email != null && email.equalsIgnoreCase(event.getCreatorEmail());
    }

    private String currentUserEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            String email = jwt.getClaim("email");
            return email != null ? email : jwt.getClaim("preferred_username");
        }
        return null;
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaim("preferred_username");
        }
        return null;
    }

    private String currentUserRole() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return null;
        }
        return authentication.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .filter(a -> a.startsWith("ROLE_"))
                .map(a -> a.substring(5))
                .filter(r -> !r.equals("offline_access")
                        && !r.equals("uma_authorization")
                        && !r.startsWith("default-roles"))
                .findFirst()
                .orElse(null);
    }

    // ==========================================
    // DISPONIBILITÉ PARTICIPANT
    // ==========================================
    @Override
    public boolean isParticipantAvailable(UUID participantId, LocalDate date,
                                          LocalTime startTime, LocalTime endTime) {
        if (!participantRepository.existsById(participantId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Participant non trouvé : " + participantId);
        }

        Schedule temp = Schedule.builder()
                .dateJour(date).startTime(startTime).endTime(endTime).build();

        for (Schedule s : scheduleRepository.findAllSchedulesByParticipantId(participantId)) {
            if (hasTimeConflict(temp, s)) {
                log.warn("Participant {} non dispo : conflit avec l'événement {}",
                        participantId, s.getEvent().getId());
                return false;
            }
        }
        return true;
    }

    // ==========================================
    // EVENTS PAR PARTICIPANT
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public List<EventDto> getEventsByParticipant(UUID participantId) {
        return eventRepository.findEventsByParticipantId(participantId).stream()
                .map(e -> enrichWithStructures(eventMapper.toDto(e), e))
                .toList();
    }

    // ==========================================
    // MÉTHODES PRIVÉES
    // ==========================================

    private List<UUID[]> processParticipants(Event event,
                                             List<ParticipantDto> dtos,
                                             List<Schedule> newSchedules) {
        List<UUID[]> invitations = new ArrayList<>();

        List<Participant> resolved = new ArrayList<>();
        for (ParticipantDto dto : dtos) {
            resolved.add(resolveParticipant(dto));
        }

        // Vérifier les doublons d'email
        Set<String> emails = new HashSet<>();
        for (Participant p : resolved) {
            if (!emails.add(p.getEmail().toLowerCase())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Email en double : " + p.getEmail());
            }
        }

        for (Participant p : resolved) {
            validateParticipantAvailability(p, newSchedules);

            if (!participantEventRepository.existsByParticipantIdAndEventId(
                    p.getId(), event.getId())) {
                participantEventRepository.save(ParticipantEvent.builder()
                        .participant(p).event(event)
                        .createdAt(LocalDateTime.now()).build());
                invitations.add(new UUID[]{event.getId(), p.getId()});
                log.info("Participant {} {} ajouté", p.getFirstName(), p.getLastName());
            }
        }
        return invitations;
    }

    /**
     * Résout un participant DTO : réutilise le participant existant si un ID est fourni
     * (évite de créer un doublon fantôme quand l'email n'est pas renseigné), sinon
     * recherche/crée par email.
     */
    private Participant resolveParticipant(ParticipantDto dto) {
        if (dto.getId() != null) {
            return participantRepository.findById(dto.getId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND, "Participant non trouvé : " + dto.getId()));
        }
        return findOrCreateParticipant(dto);
    }

    private Participant findOrCreateParticipant(ParticipantDto dto) {
        return participantRepository.findByEmail(dto.getEmail())
                .orElseGet(() -> {
                    Participant p = participantMapper.toEntity(dto);
                    p.setCreatedAt(LocalDateTime.now());
                    p = participantRepository.save(p);
                    log.info("Nouveau participant : {} {}", p.getFirstName(), p.getLastName());
                    return p;
                });
    }

    private void validateParticipantAvailability(Participant participant,
                                                 List<Schedule> newSchedules) {
        List<Schedule> existing = scheduleRepository
                .findAllSchedulesByParticipantId(participant.getId());
        if (existing.isEmpty()) return;

        DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        DateTimeFormatter tf = DateTimeFormatter.ofPattern("HH:mm");

        for (Schedule ns : newSchedules) {
            for (Schedule es : existing) {
                if (hasTimeConflict(ns, es)) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            String.format(
                                    "Impossible d'inscrire %s %s : " +
                                            "il/elle participe déjà à « %s » " +
                                            "le %s de %s à %s, " +
                                            "en conflit avec %s-%s. " +
                                            "Veuillez choisir un autre participant " +
                                            "ou modifier les horaires.",
                                    participant.getFirstName(),
                                    participant.getLastName(),
                                    es.getEvent().getTitle(),
                                    es.getDateJour().format(df),
                                    es.getStartTime().format(tf),
                                    es.getEndTime().format(tf),
                                    ns.getStartTime().format(tf),
                                    ns.getEndTime().format(tf)));
                }
            }
        }
    }

    private boolean hasTimeConflict(Schedule s1, Schedule s2) {
        if (!s1.getDateJour().equals(s2.getDateJour())) return false;
        boolean overlap = s1.getStartTime().isBefore(s2.getEndTime()) &&
                s2.getStartTime().isBefore(s1.getEndTime());
        if (overlap) {
            log.warn("Chevauchement : {} ({}-{}) vs ({}-{})",
                    s1.getDateJour(), s1.getStartTime(), s1.getEndTime(),
                    s2.getStartTime(), s2.getEndTime());
        }
        return overlap;
    }

    private List<Schedule> createSchedules(Event event, EventDto dto) {
        List<Schedule> schedules = new ArrayList<>();

        if (dto.isGlobalScheduleMode()) {
            validateTimeRange(dto.getGlobalStartTime(), dto.getGlobalEndTime());
            LocalDate cur = dto.getStartDate();
            while (!cur.isAfter(dto.getEndDate())) {
                schedules.add(Schedule.builder()
                        .dateJour(cur)
                        .startTime(dto.getGlobalStartTime())
                        .endTime(dto.getGlobalEndTime())
                        .event(event)
                        .createdAt(LocalDateTime.now())
                        .build());
                cur = cur.plusDays(1);
            }
            schedules = scheduleRepository.saveAll(schedules);
            log.info("{} horaires globaux", schedules.size());

        } else if (dto.isCustomScheduleMode()) {
            for (ScheduleDto s : dto.getSchedules()) {
                validateScheduleInPeriod(s, dto.getStartDate(), dto.getEndDate());
                validateTimeRange(s.getStartTime(), s.getEndTime());
                schedules.add(Schedule.builder()
                        .dateJour(s.getDateJour())
                        .startTime(s.getStartTime())
                        .endTime(s.getEndTime())
                        .address(s.getAddress())
                        .event(event)
                        .createdAt(LocalDateTime.now())
                        .build());
            }
            schedules = scheduleRepository.saveAll(schedules);
            log.info("{} horaires custom", schedules.size());
        }
        return schedules;
    }

    private void validateEventDates(LocalDate start, LocalDate end) {
        if (!ValidationUtils.isValidDateRange(start, end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La date de fin doit être >= à la date de début");
        }
    }

    private void validateTimeRange(LocalTime start, LocalTime end) {
        if (!ValidationUtils.isValidTimeRange(start, end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "L'heure de fin doit être > à l'heure de début");
        }
    }

    private void validateScheduleMode(EventDto dto) {
        boolean hasGlobal = dto.getGlobalStartTime() != null &&
                dto.getGlobalEndTime()   != null;
        boolean hasCustom = dto.getSchedules() != null &&
                !dto.getSchedules().isEmpty();

        log.info("validateScheduleMode → globalStart={} globalEnd={} " +
                        "schedules.size={} isGlobal={} isCustom={}",
                dto.getGlobalStartTime(), dto.getGlobalEndTime(),
                dto.getSchedules() != null ? dto.getSchedules().size() : "null",
                dto.isGlobalScheduleMode(), dto.isCustomScheduleMode());

        if (!hasGlobal && !hasCustom) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Fournir globalStartTime+globalEndTime OU schedules");
        }
        if (hasGlobal && hasCustom) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ne pas fournir à la fois horaires globaux ET spécifiques");
        }
    }

    private void validateScheduleInPeriod(ScheduleDto s,
                                          LocalDate start, LocalDate end) {
        if (s.getDateJour().isBefore(start) || s.getDateJour().isAfter(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    String.format("Date %s hors période (%s - %s)",
                            s.getDateJour(), start, end));
        }
    }

    // ==========================================
    // PDF
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public byte[] generateAttendanceSheet(UUID eventId) {
        log.info("Génération PDF émargement : {}", eventId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Événement non trouvé : " + eventId));

        List<ParticipantEvent> pes = new ArrayList<>(event.getParticipantEvents());

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfDocument pdfDoc = new PdfDocument(new PdfWriter(baos));
            Document doc = new Document(pdfDoc, PageSize.A4);
            doc.setMargins(20, 30, 30, 30);

            PdfFont bold   = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont normal = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            addPdfHeader(doc, bold, normal);
            doc.add(new Paragraph("\n"));
            doc.add(new Paragraph("LISTE PARTICIPANT")
                    .setFont(bold).setFontSize(16)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(10).setMarginBottom(20).setUnderline());

            addEventInfoPdf(doc, event, bold, normal);
            doc.add(new Paragraph("\n"));
            addParticipantsTablePdf(doc, pes, bold, normal);
            addSignatureSectionPdf(doc, normal);
            doc.close();

            log.info("✅ PDF généré");
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("❌ Erreur PDF : {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur génération PDF : " + e.getMessage());
        }
    }

    private void addPdfHeader(Document doc, PdfFont bold, PdfFont normal)
            throws Exception {
        com.itextpdf.layout.element.Table t =
                new com.itextpdf.layout.element.Table(
                        UnitValue.createPercentArray(new float[]{35, 30, 35}));
        t.setWidth(UnitValue.createPercentValue(100));
        DeviceRgb bc = new DeviceRgb(150, 150, 150);

        com.itextpdf.layout.element.Cell left =
                new com.itextpdf.layout.element.Cell()
                        .setBorder(new SolidBorder(bc, 1)).setPadding(10)
                        .setVerticalAlignment(VerticalAlignment.MIDDLE);
        left.add(new Paragraph()
                .add(new Text("AUTORITE SUPERIEURE DE\n").setFont(bold).setFontSize(9))
                .add(new Text("CONTROLE D'ETAT ET DE LUTTE\n").setFont(bold).setFontSize(9))
                .add(new Text("CONTRE LA CORRUPTION\n").setFont(bold).setFontSize(9))
                .add(new Text("------------\n").setFont(normal).setFontSize(8))
                .add(new Text("SECRETARIAT GENERAL\n").setFont(bold).setFontSize(9))
                .add(new Text("------------\n").setFont(normal).setFontSize(8))
                .add(new Text("DIRECTION DES SYSTEMES\n").setFont(bold).setFontSize(8))
                .add(new Text("D'INFORMATION, DE LA\n").setFont(bold).setFontSize(8))
                .add(new Text("DOCUMENTATION ET DES\n").setFont(bold).setFontSize(8))
                .add(new Text("ARCHIVES").setFont(bold).setFontSize(8))
                .setTextAlignment(TextAlignment.CENTER));

        com.itextpdf.layout.element.Cell center =
                new com.itextpdf.layout.element.Cell()
                        .setBorder(Border.NO_BORDER).setPadding(5)
                        .setVerticalAlignment(VerticalAlignment.MIDDLE)
                        .setHorizontalAlignment(HorizontalAlignment.CENTER);
        try {
            ClassPathResource logo = new ClassPathResource("static/images/logo.png");
            if (logo.exists()) {
                center.add(new Image(ImageDataFactory.create(logo.getURL()))
                        .setWidth(80).setHorizontalAlignment(HorizontalAlignment.CENTER));
            } else {
                center.add(new Paragraph("ASCELC").setFont(bold).setFontSize(20)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontColor(new DeviceRgb(0, 100, 0)));
            }
        } catch (Exception e) {
            center.add(new Paragraph("ASCELC").setFont(bold).setFontSize(20)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(new DeviceRgb(0, 100, 0)));
        }

        com.itextpdf.layout.element.Cell right =
                new com.itextpdf.layout.element.Cell()
                        .setBorder(new SolidBorder(bc, 1)).setPadding(10)
                        .setVerticalAlignment(VerticalAlignment.MIDDLE);
        right.add(new Paragraph()
                .add(new Text("BURKINA FASO\n").setFont(bold).setFontSize(11))
                .add(new Text("------------\n").setFont(normal).setFontSize(8))
                .add(new Text("La Patrie ou la Mort,\n")
                        .setFont(normal).setFontSize(9).setItalic())
                .add(new Text("nous Vaincrons")
                        .setFont(normal).setFontSize(9).setItalic())
                .setTextAlignment(TextAlignment.CENTER));

        t.addCell(left); t.addCell(center); t.addCell(right);
        doc.add(t);
    }

    private void addEventInfoPdf(Document doc, Event event,
                                 PdfFont bold, PdfFont normal) {
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        com.itextpdf.layout.element.Table info =
                new com.itextpdf.layout.element.Table(
                        UnitValue.createPercentArray(new float[]{30, 70}));
        info.setWidth(UnitValue.createPercentValue(100)).setMarginBottom(10);

        info.addCell(createInfoCell("Titre :", bold));
        info.addCell(createInfoCell(event.getTitle(), normal));
        info.addCell(createInfoCell("Type :", bold));
        info.addCell(createInfoCell(getTypeLabel(event.getType().name()), normal));

        String dates = event.getStartDate().format(fmt);
        if (!event.getStartDate().equals(event.getEndDate()))
            dates += " au " + event.getEndDate().format(fmt);
        info.addCell(createInfoCell("Date :", bold));
        info.addCell(createInfoCell(dates, normal));

        String lieu = "";
        if (event.getVille() != null) lieu += event.getVille();
        if (event.getPays()  != null) {
            if (!lieu.isEmpty()) lieu += ", ";
            lieu += event.getPays();
        }
        if (!lieu.isEmpty()) {
            info.addCell(createInfoCell("Lieu :", bold));
            info.addCell(createInfoCell(lieu, normal));
        }
        doc.add(info);
    }

    private com.itextpdf.layout.element.Cell createInfoCell(
            String text, PdfFont font) {
        return new com.itextpdf.layout.element.Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(10))
                .setBorder(Border.NO_BORDER).setPadding(3);
    }

    private void addParticipantsTablePdf(Document doc,
                                         List<ParticipantEvent> pes,
                                         PdfFont bold, PdfFont normal) {
        if (pes == null || pes.isEmpty()) {
            doc.add(new Paragraph("Aucun participant enregistré")
                    .setFont(normal).setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.GRAY));
            return;
        }

        com.itextpdf.layout.element.Table table =
                new com.itextpdf.layout.element.Table(
                        UnitValue.createPercentArray(new float[]{8, 30, 30, 32}));
        table.setWidth(UnitValue.createPercentValue(100));
        DeviceRgb hc = new DeviceRgb(220, 220, 220);

        table.addHeaderCell(createHeaderCell("N°",                   bold, hc));
        table.addHeaderCell(createHeaderCell("Nom et Prénoms",       bold, hc));
        table.addHeaderCell(createHeaderCell("Structure / Fonction", bold, hc));
        table.addHeaderCell(createHeaderCell("Signature",            bold, hc));

        int i = 1;
        for (ParticipantEvent pe : pes) {
            Participant p = pe.getParticipant();
            table.addCell(createDataCell(String.valueOf(i++), normal, TextAlignment.CENTER));
            table.addCell(createDataCell(
                    p.getFirstName() + " " + p.getLastName(), normal, TextAlignment.LEFT));
            String org = "";
            if (p.getStructure() != null) org  = p.getStructure();
            if (p.getJobTitle()  != null) {
                if (!org.isEmpty()) org += "\n";
                org += p.getJobTitle();
            }
            table.addCell(createDataCell(
                    org.isEmpty() ? "-" : org, normal, TextAlignment.LEFT));
            table.addCell(createDataCell("", normal, TextAlignment.CENTER));
        }
        doc.add(table);
    }

    private com.itextpdf.layout.element.Cell createHeaderCell(
            String text, PdfFont font, DeviceRgb bg) {
        return new com.itextpdf.layout.element.Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(9).setBold())
                .setBackgroundColor(bg)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(8)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1));
    }

    private com.itextpdf.layout.element.Cell createDataCell(
            String text, PdfFont font, TextAlignment align) {
        return new com.itextpdf.layout.element.Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(9))
                .setTextAlignment(align)
                .setPadding(8).setMinHeight(35)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f));
    }

    private void addSignatureSectionPdf(Document doc, PdfFont normal) {
        doc.add(new Paragraph("\n\n"));
        com.itextpdf.layout.element.Table sig =
                new com.itextpdf.layout.element.Table(
                        UnitValue.createPercentArray(new float[]{50, 50}));
        sig.setWidth(UnitValue.createPercentValue(100));
        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        sig.addCell(new com.itextpdf.layout.element.Cell()
                .add(new Paragraph("Fait à Ouagadougou, le " + today)
                        .setFont(normal).setFontSize(10))
                .setBorder(Border.NO_BORDER));
        sig.addCell(new com.itextpdf.layout.element.Cell()
                .add(new Paragraph("Le Responsable")
                        .setFont(normal).setFontSize(10)
                        .setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph("\n\n\n").setFont(normal).setFontSize(10))
                .setBorder(Border.NO_BORDER));
        doc.add(sig);
    }

    private String getTypeLabel(String type) {
        return switch (type) {
            case "REUNION"    -> "Réunion";
            case "CONFERENCE" -> "Conférence";
            case "ATELIER"    -> "Atelier";
            case "SEMINAIRE"  -> "Séminaire";
            case "FORMATION"  -> "Formation";
            case "MISSION"    -> "Mission";
            case "AUTRE"      -> "Autre";
            default           -> type;
        };
    }

    private List<ParticipantDto> parseCSV(byte[] content) throws Exception {
        List<ParticipantDto> list = new ArrayList<>();
        try (CSVParser p = CSVFormat.DEFAULT.builder()
                .setHeader().setSkipHeaderRecord(true).build()
                .parse(new InputStreamReader(
                        new ByteArrayInputStream(content), StandardCharsets.UTF_8))) {
            for (CSVRecord r : p) {
                list.add(ParticipantDto.builder()
                        .lastName(r.get("Nom"))
                        .firstName(r.get("Prénom"))
                        .email(r.get("Email"))
                        .phoneNumber(r.get("Téléphone"))
                        .structure(r.get("Structure"))
                        .jobTitle(r.get("Fonction"))
                        .build());
            }
        }
        return list;
    }

    private List<ParticipantDto> parseExcel(byte[] content) throws Exception {
        List<ParticipantDto> list = new ArrayList<>();
        try (Workbook wb = WorkbookFactory.create(
                new ByteArrayInputStream(content))) {
            Sheet sheet = wb.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    list.add(ParticipantDto.builder()
                            .lastName(getCellValue(row.getCell(0)))
                            .firstName(getCellValue(row.getCell(1)))
                            .email(getCellValue(row.getCell(2)))
                            .phoneNumber(getCellValue(row.getCell(3)))
                            .structure(getCellValue(row.getCell(4)))
                            .jobTitle(getCellValue(row.getCell(5)))
                            .build());
                }
            }
        }
        return list;
    }

    private String getCellValue(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default      -> "";
        };
    }

    private EventDto enrichWithStructures(EventDto dto, Event event) {
        dto.setStructures(extractUniqueStructures(event));
        return dto;
    }

    private List<String> extractUniqueStructures(Event event) {
        return event.getParticipantEvents().stream()
                .map(pe -> pe.getParticipant().getStructure())
                .filter(Objects::nonNull)
                .distinct().sorted().toList();
    }


    @Override
    @Transactional
    public void updateStatusOnly(UUID id, String status) {
        eventRepository.findById(id).ifPresent(event -> {
            try {
                EventStatus newStatus = EventStatus.valueOf(status);
                // ✅ Pas de TransactionSynchronization → pas d'email
                event.setStatus(newStatus);
                event.setUpdatedAt(LocalDateTime.now());
                eventRepository.save(event);
                log.info("✅ Statut auto mis à jour : {} → {}", id, status);
            } catch (IllegalArgumentException e) {
                log.error("❌ Statut invalide : {}", status);
            }
        });
    }
}