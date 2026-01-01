package gov.bf.ascelc.cge_agenda.service.impl;

import gov.bf.ascelc.cge_agenda.dto.*;
import gov.bf.ascelc.cge_agenda.entities.Event;
import gov.bf.ascelc.cge_agenda.entities.Participant;
import gov.bf.ascelc.cge_agenda.entities.ParticipantEvent;
import gov.bf.ascelc.cge_agenda.entities.Schedule;
import gov.bf.ascelc.cge_agenda.enums.EventStatus;
import gov.bf.ascelc.cge_agenda.enums.EventType;
import gov.bf.ascelc.cge_agenda.mapper.EventMapper;
import gov.bf.ascelc.cge_agenda.mapper.ParticipantMapper;
import gov.bf.ascelc.cge_agenda.repository.EventRepository;
import gov.bf.ascelc.cge_agenda.repository.ParticipantEventRepository;
import gov.bf.ascelc.cge_agenda.repository.ParticipantRepository;
import gov.bf.ascelc.cge_agenda.repository.ScheduleRepository;
import gov.bf.ascelc.cge_agenda.service.EventService;

import gov.bf.ascelc.cge_agenda.service.PdfService;
import gov.bf.ascelc.cge_agenda.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.*;
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

    // ==========================================
    // CRÉATION D'UN ÉVÉNEMENT COMPLET
    // ==========================================
    @Override
    @Transactional(rollbackFor = Exception.class)
    public EventDto create(EventDto dto) {
        log.info("Début création événement : {}", dto.getTitle());

        validateEventDates(dto.getStartDate(), dto.getEndDate());
        validateScheduleMode(dto);

        Event event = eventMapper.toEntity(dto);
        event.setCreatedAt(LocalDateTime.now());

        event = eventRepository.save(event);
        log.info("Événement créé : ID = {}", event.getId());

        List<Schedule> schedules = createSchedules(event, dto);
        log.info("{} horaires créés", schedules.size());

        if (dto.getParticipants() != null && !dto.getParticipants().isEmpty()) {
            processParticipants(event, dto.getParticipants(), schedules);
        }

        log.info("Événement créé avec succès !");
        return getEventById(event.getId());
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
                    log.info("Événement annulé : ID = {}", id);

                    return eventMapper.toDto(updated);
                })
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Événement non trouvé avec l'ID : " + id
                ));
    }

    // ==========================================
    // REPORTER UN ÉVÉNEMENT
    // ==========================================

    @Override
    public EventDto postponeEvent(UUID id, LocalDate newStartDate, LocalDate newEndDate) {
        log.info("Report de l'événement : ID = {}, nouvelles dates : {} - {}",
                id, newStartDate, newEndDate);

        validateEventDates(newStartDate, newEndDate);

        return eventRepository.findById(id)
                .map(event -> {
                    // Sauvegarder les horaires globaux s'ils existaient
                    LocalTime globalStartTime = null;
                    LocalTime globalEndTime = null;

                    if (!event.getSchedules().isEmpty()) {
                        //Utiliser stream().findFirst() au lieu de get(0)
                        Schedule firstSchedule = event.getSchedules()
                                .stream()
                                .findFirst()
                                .orElse(null);

                        if (firstSchedule != null) {
                            globalStartTime = firstSchedule.getStartTime();
                            globalEndTime = firstSchedule.getEndTime();
                        }
                    }

                    // Modifier l'événement
                    event.setStatus(EventStatus.REPORTER);
                    event.setStartDate(newStartDate);
                    event.setEndDate(newEndDate);
                    event.setUpdatedAt(LocalDateTime.now());

                    // Vider la collection de schedules
                    event.getSchedules().clear();

                    // Sauvegarder
                    Event updated = eventRepository.save(event);

                    // Supprimer les anciens schedules
                    scheduleRepository.deleteByEventId(id);

                    // Recréer automatiquement les horaires si mode global
                    if (globalStartTime != null && globalEndTime != null) {
                        LocalDate currentDate = newStartDate;
                        while (!currentDate.isAfter(newEndDate)) {
                            Schedule schedule = Schedule.builder()
                                    .dateJour(currentDate)
                                    .startTime(globalStartTime)
                                    .endTime(globalEndTime)
                                    .event(updated)
                                    .createdAt(LocalDateTime.now())
                                    .build();

                            scheduleRepository.save(schedule);
                            currentDate = currentDate.plusDays(1);
                        }
                        log.info("Horaires recréés pour les nouvelles dates");
                    }

                    log.info("Événement reporté : ID = {}", id);
                    return eventMapper.toDto(updated);
                })
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Événement non trouvé avec l'ID : " + id
                ));
    }

    // ==========================================
    // RECHERCHE D'ÉVÉNEMENTS
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public List<EventDto> searchEvents(String keyword, EventType type, EventStatus status,
                                       LocalDate startDate, LocalDate endDate) {
        log.info("Recherche d'événements : keyword={}, type={}, status={}", keyword, type, status);

        List<Event> events = eventRepository.findAll();

        // Filtrer par mot-clé
        if (keyword != null && !keyword.trim().isEmpty()) {
            String lowerKeyword = keyword.toLowerCase();
            events = events.stream()
                    .filter(e -> e.getTitle().toLowerCase().contains(lowerKeyword) ||
                            (e.getDescription() != null &&
                                    e.getDescription().toLowerCase().contains(lowerKeyword)))
                    .collect(Collectors.toList());
        }

        // Filtrer par type
        if (type != null) {
            events = events.stream()
                    .filter(e -> e.getType() == type)
                    .collect(Collectors.toList());
        }

        // Filtrer par statut
        if (status != null) {
            events = events.stream()
                    .filter(e -> e.getStatus() == status)
                    .collect(Collectors.toList());
        }

        // Filtrer par période
        if (startDate != null && endDate != null) {
            events = events.stream()
                    .filter(e -> !e.getEndDate().isBefore(startDate) &&
                            !e.getStartDate().isAfter(endDate))
                    .collect(Collectors.toList());
        }

        log.info("{} événements trouvés", events.size());
        return eventMapper.toDtos(events);
    }

    // ==========================================
    // ÉVÉNEMENTS PAR MOIS
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public List<EventDto> getEventsByMonth(int year, int month) {
        log.info("Récupération des événements pour : {}/{}", month, year);

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth();

        return getEventsByDateRange(startDate, endDate);
    }

    // ==========================================
    // ÉVÉNEMENTS PAR PÉRIODE
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public List<EventDto> getEventsByDateRange(LocalDate startDate, LocalDate endDate) {
        log.info("Récupération des événements entre {} et {}", startDate, endDate);

        List<Event> events = eventRepository.findAll().stream()
                .filter(e -> !e.getEndDate().isBefore(startDate) &&
                        !e.getStartDate().isAfter(endDate))
                .collect(Collectors.toList());

        log.info("{} événements trouvés", events.size());
        return eventMapper.toDtos(events);
    }

    // ==========================================
    // AJOUTER UN PARTICIPANT
    // ==========================================
    @Override
    @Transactional
    public EventDto addParticipant(UUID eventId, ParticipantDto participantDto) {
        log.info("Ajout d'un participant à l'événement : {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Événement non trouvé avec l'ID : " + eventId
                ));

        List<Schedule> schedules = scheduleRepository.findByEventId(eventId);

        Participant participant;

        // ✅ AJOUT : Si le DTO contient un ID, utiliser le participant existant
        if (participantDto.getId() != null) {
            log.info("Ajout du participant existant : ID = {}", participantDto.getId());
            participant = participantRepository.findById(participantDto.getId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Participant non trouvé avec l'ID : " + participantDto.getId()
                    ));
        } else {
            // Sinon, créer ou trouver par email
            participant = findOrCreateParticipant(participantDto);
        }

        validateParticipantAvailability(participant, schedules);

        // Vérifier si déjà inscrit
        if (participantEventRepository.existsByParticipantIdAndEventId(
                participant.getId(), eventId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    String.format("Le participant %s %s est déjà inscrit à cet événement",
                            participant.getFirstName(), participant.getLastName())
            );
        }

        // Ajouter le participant à l'événement
        ParticipantEvent participantEvent = ParticipantEvent.builder()
                .participant(participant)
                .event(event)
                .createdAt(LocalDateTime.now())
                .build();

        participantEventRepository.save(participantEvent);
        log.info("Participant ajouté : {} {}", participant.getFirstName(),
                participant.getLastName());

        return getEventById(eventId);
    }


    // ==========================================
    // RETIRER UN PARTICIPANT
    // ==========================================
    @Override
    public EventDto removeParticipant(UUID eventId, UUID participantId) {
        log.info("Retrait du participant {} de l'événement {}", participantId, eventId);

        if (!eventRepository.existsById(eventId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Événement non trouvé avec l'ID : " + eventId
            );
        }

        participantEventRepository.findAll().stream()
                .filter(pe -> pe.getEvent().getId().equals(eventId) &&
                        pe.getParticipant().getId().equals(participantId))
                .findFirst()
                .ifPresent(participantEventRepository::delete);

        log.info("Participant retiré avec succès");
        return getEventById(eventId);
    }

    // ==========================================
    // RÉCUPÉRER LES PARTICIPANTS D'UN ÉVÉNEMENT
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public List<ParticipantDto> getEventParticipants(UUID eventId) {
        log.info("Récupération des participants de l'événement : {}", eventId);

        if (!eventRepository.existsById(eventId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Événement non trouvé avec l'ID : " + eventId
            );
        }

        List<Participant> participants = participantEventRepository
                .findParticipantsByEventId(eventId);

        log.info("{} participants trouvés", participants.size());
        return participantMapper.toDtos(participants);
    }

    // ==========================================
    // IMPORTER DES PARTICIPANTS
    // ==========================================
    @Override
    public List<ParticipantDto> importParticipants(UUID eventId, byte[] fileContent,
                                                   String fileType) {
        log.info("Import de participants pour l'événement : {}, type : {}", eventId, fileType);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Événement non trouvé avec l'ID : " + eventId
                ));

        List<Schedule> schedules = scheduleRepository.findByEventId(eventId);
        List<ParticipantDto> importedParticipants = new ArrayList<>();

        try {
            List<ParticipantDto> participantsToImport;

            if ("csv".equalsIgnoreCase(fileType)) {
                participantsToImport = parseCSV(fileContent);
            } else if ("xlsx".equalsIgnoreCase(fileType) || "xls".equalsIgnoreCase(fileType)) {
                participantsToImport = parseExcel(fileContent);
            } else {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Format de fichier non supporté : " + fileType
                );
            }

            for (ParticipantDto dto : participantsToImport) {
                try {
                    Participant participant = findOrCreateParticipant(dto);
                    validateParticipantAvailability(participant, schedules);

                    if (!participantEventRepository.existsByParticipantIdAndEventId(
                            participant.getId(), eventId)) {

                        ParticipantEvent participantEvent = ParticipantEvent.builder()
                                .participant(participant)
                                .event(event)
                                .createdAt(LocalDateTime.now())
                                .build();

                        participantEventRepository.save(participantEvent);
                        importedParticipants.add(participantMapper.toDto(participant));
                    }
                } catch (Exception e) {
                    log.warn("Erreur lors de l'import du participant {} : {}",
                            dto.getEmail(), e.getMessage());
                }
            }

            log.info("{} participants importés avec succès", importedParticipants.size());
            return importedParticipants;

        } catch (Exception e) {
            log.error("Erreur lors de l'import : {}", e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors de l'import des participants : " + e.getMessage()
            );
        }
    }

    // ==========================================
    // GÉNÉRER LISTE D'ÉMARGEMENT
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public byte[] generateAttendanceSheet(UUID eventId) {
        log.info("Génération de la liste d'émargement pour l'événement : {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Événement non trouvé avec l'ID : " + eventId
                ));

        List<Participant> participants = participantEventRepository
                .findParticipantsByEventId(eventId);

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Liste d'émargement");

            // En-tête
            Row headerRow = sheet.createRow(0);
            headerRow.createCell(0).setCellValue("Événement : " + event.getTitle());

            Row dateRow = sheet.createRow(1);
            dateRow.createCell(0).setCellValue("Du " + event.getStartDate() +
                    " au " + event.getEndDate());

            // En-tête du tableau
            Row tableHeaderRow = sheet.createRow(3);
            tableHeaderRow.createCell(0).setCellValue("N°");
            tableHeaderRow.createCell(1).setCellValue("Nom");
            tableHeaderRow.createCell(2).setCellValue("Prénom");
            tableHeaderRow.createCell(3).setCellValue("Organisation");
            tableHeaderRow.createCell(4).setCellValue("Email");
            tableHeaderRow.createCell(5).setCellValue("Signature");

            // Données
            int rowNum = 4;
            int index = 1;
            for (Participant participant : participants) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(index++);
                row.createCell(1).setCellValue(participant.getLastName());
                row.createCell(2).setCellValue(participant.getFirstName());
                row.createCell(3).setCellValue(participant.getOrganization());
                row.createCell(4).setCellValue(participant.getEmail());
                row.createCell(5).setCellValue("");
            }

            // Auto-size colonnes
            for (int i = 0; i < 6; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);

            log.info("Liste d'émargement générée avec succès");
            return outputStream.toByteArray();

        } catch (Exception e) {
            log.error("Erreur lors de la génération de la liste d'émargement : {}",
                    e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors de la génération de la liste d'émargement"
            );
        }
    }

    @Override
    public boolean isParticipantAvailable(UUID participantId, LocalDate date,
                                          LocalTime startTime, LocalTime endTime) {
        log.info("Vérification disponibilité participant : {}", participantId);

        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Participant non trouvé avec l'ID : " + participantId
                ));

        // Créer un horaire temporaire pour la vérification
        Schedule tempSchedule = Schedule.builder()
                .dateJour(date)
                .startTime(startTime)
                .endTime(endTime)
                .build();

        List<Event> existingEvents = participantEventRepository
                .findEventsByParticipantId(participantId);

        for (Event existingEvent : existingEvents) {
            List<Schedule> existingSchedules = scheduleRepository
                    .findByEventId(existingEvent.getId());

            for (Schedule existingSchedule : existingSchedules) {
                if (hasTimeConflict(tempSchedule, existingSchedule)) {
                    log.warn("Participant {} non disponible : conflit avec événement '{}'",
                            participantId, existingEvent.getTitle());
                    return false;
                }
            }
        }

        log.info("Participant {} disponible pour la période demandée", participantId);
        return true;
    }

    // ==========================================
    // MÉTHODES UTILITAIRES PRIVÉES
    // ==========================================

    private List<ParticipantDto> parseCSV(byte[] fileContent) throws Exception {
        List<ParticipantDto> participants = new ArrayList<>();

        try (CSVParser parser = CSVFormat.DEFAULT
                .builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .build()
                .parse(new InputStreamReader(new ByteArrayInputStream(fileContent),
                        StandardCharsets.UTF_8))) {

            for (CSVRecord record : parser) {
                ParticipantDto dto = ParticipantDto.builder()
                        .lastName(record.get("Nom"))
                        .firstName(record.get("Prénom"))
                        .email(record.get("Email"))
                        .phoneNumber(record.get("Téléphone"))
                        .organization(record.get("Organisation"))
                        .jobTitle(record.get("Fonction"))
                        .build();

                participants.add(dto);
            }
        }

        return participants;
    }

    private List<ParticipantDto> parseExcel(byte[] fileContent) throws Exception {
        List<ParticipantDto> participants = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(fileContent))) {
            Sheet sheet = workbook.getSheetAt(0);

            // Ignorer la première ligne (en-tête)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row != null) {
                    ParticipantDto dto = ParticipantDto.builder()
                            .lastName(getCellValue(row.getCell(0)))
                            .firstName(getCellValue(row.getCell(1)))
                            .email(getCellValue(row.getCell(2)))
                            .phoneNumber(getCellValue(row.getCell(3)))
                            .organization(getCellValue(row.getCell(4)))
                            .jobTitle(getCellValue(row.getCell(5)))
                            .build();

                    participants.add(dto);
                }
            }
        }

        return participants;
    }

    private String getCellValue(Cell cell) {
        if (cell == null) {
            return "";
        }

        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            default -> "";
        };
    }

    private List<Schedule> createSchedules(Event event, EventDto dto) {
        List<Schedule> schedules = new ArrayList<>();

        if (dto.isGlobalScheduleMode()) {
            validateTimeRange(dto.getGlobalStartTime(), dto.getGlobalEndTime());

            LocalDate currentDate = dto.getStartDate();
            while (!currentDate.isAfter(dto.getEndDate())) {
                Schedule schedule = Schedule.builder()
                        .dateJour(currentDate)
                        .startTime(dto.getGlobalStartTime())
                        .endTime(dto.getGlobalEndTime())
                        .event(event)
                        .createdAt(LocalDateTime.now())
                        .build();

                schedules.add(scheduleRepository.save(schedule));
                currentDate = currentDate.plusDays(1);
            }

            log.info("{} horaires globaux générés", schedules.size());
        } else if (dto.isCustomScheduleMode()) {
            for (ScheduleDto scheduleDto : dto.getSchedules()) {
                validateScheduleInPeriod(scheduleDto, dto.getStartDate(), dto.getEndDate());
                validateTimeRange(scheduleDto.getStartTime(), scheduleDto.getEndTime());

                Schedule schedule = Schedule.builder()
                        .dateJour(scheduleDto.getDateJour())
                        .startTime(scheduleDto.getStartTime())
                        .endTime(scheduleDto.getEndTime())
                        .address(scheduleDto.getAddress())
                        .event(event)
                        .createdAt(LocalDateTime.now())
                        .build();

                schedules.add(scheduleRepository.save(schedule));
            }

            log.info("{} horaires spécifiques créés", schedules.size());
        }

        return schedules;
    }

    //  AMÉLIORATION : Vérifier les doublons d'emails
    private void processParticipants(Event event, List<ParticipantDto> participantDtos,
                                     List<Schedule> newSchedules) {
        // Vérifier les doublons dans la liste
        Set<String> emails = new HashSet<>();
        for (ParticipantDto dto : participantDtos) {
            String email = dto.getEmail().toLowerCase();
            if (!emails.add(email)) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Email en double dans la liste des participants : " + dto.getEmail()
                );
            }
        }

        // Ajouter les participants
        for (ParticipantDto participantDto : participantDtos) {
            Participant participant = findOrCreateParticipant(participantDto);
            validateParticipantAvailability(participant, newSchedules);

            if (!participantEventRepository.existsByParticipantIdAndEventId(
                    participant.getId(), event.getId())) {

                ParticipantEvent participantEvent = ParticipantEvent.builder()
                        .participant(participant)
                        .event(event)
                        .createdAt(LocalDateTime.now())
                        .build();

                participantEventRepository.save(participantEvent);
                log.info("Participant {} {} ajouté à l'événement",
                        participant.getFirstName(), participant.getLastName());
            }
        }
    }

    private Participant findOrCreateParticipant(ParticipantDto dto) {
        Optional<Participant> existing = participantRepository.findByEmail(dto.getEmail());

        if (existing.isPresent()) {
            log.info("Participant existant trouvé : {}", dto.getEmail());
            return existing.get();
        }

        Participant newParticipant = participantMapper.toEntity(dto);
        newParticipant.setCreatedAt(LocalDateTime.now());

        newParticipant = participantRepository.save(newParticipant);
        log.info("Nouveau participant créé : {} {}",
                newParticipant.getFirstName(), newParticipant.getLastName());

        return newParticipant;
    }

    private void validateParticipantAvailability(Participant participant,
                                                 List<Schedule> newSchedules) {
        log.info("Vérification disponibilité : {} {}",
                participant.getFirstName(), participant.getLastName());

        List<Event> existingEvents = participantEventRepository
                .findEventsByParticipantId(participant.getId());

        if (existingEvents.isEmpty()) {
            log.info("Participant disponible (aucun événement existant)");
            return;
        }

        for (Event existingEvent : existingEvents) {
            List<Schedule> existingSchedules = scheduleRepository
                    .findByEventId(existingEvent.getId());

            for (Schedule newSchedule : newSchedules) {
                for (Schedule existingSchedule : existingSchedules) {
                    if (hasTimeConflict(newSchedule, existingSchedule)) {
                        String errorMessage = String.format(
                                "CONFLIT HORAIRE détecté !\n" +
                                        "Participant : %s %s\n" +
                                        "Événement existant : '%s'\n" +
                                        "Date : %s\n" +
                                        "Horaire existant : %s - %s\n" +
                                        "Nouvel horaire : %s - %s",
                                participant.getFirstName(), participant.getLastName(),
                                existingEvent.getTitle(),
                                existingSchedule.getDateJour(),
                                existingSchedule.getStartTime(), existingSchedule.getEndTime(),
                                newSchedule.getStartTime(), newSchedule.getEndTime()
                        );

                        log.error(errorMessage);
                        throw new ResponseStatusException(HttpStatus.CONFLICT, errorMessage);
                    }
                }
            }
        }

        log.info("Participant disponible (aucun conflit détecté)");
    }

    private boolean hasTimeConflict(Schedule schedule1, Schedule schedule2) {
        if (!schedule1.getDateJour().equals(schedule2.getDateJour())) {
            return false;
        }

        LocalTime start1 = schedule1.getStartTime();
        LocalTime end1 = schedule1.getEndTime();
        LocalTime start2 = schedule2.getStartTime();
        LocalTime end2 = schedule2.getEndTime();

        boolean overlap = start1.isBefore(end2) && start2.isBefore(end1);

        if (overlap) {
            log.warn("Chevauchement détecté : {} ({} - {}) vs ({} - {})",
                    schedule1.getDateJour(), start1, end1, start2, end2);
        }

        return overlap;
    }

    private void validateEventDates(LocalDate startDate, LocalDate endDate) {
        if (!ValidationUtils.isValidDateRange(startDate, endDate)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La date de fin doit être supérieure ou égale à la date de début"
            );
        }
    }

    private void validateTimeRange(LocalTime startTime, LocalTime endTime) {
        if (!ValidationUtils.isValidTimeRange(startTime, endTime)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "L'heure de fin doit être supérieure à l'heure de début"
            );
        }
    }

    private void validateScheduleMode(EventDto dto) {
        boolean hasGlobalTime = dto.getGlobalStartTime() != null &&
                dto.getGlobalEndTime() != null;
        boolean hasCustomSchedules = dto.getSchedules() != null &&
                !dto.getSchedules().isEmpty();

        if (!hasGlobalTime && !hasCustomSchedules) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vous devez fournir soit des horaires globaux, soit des horaires spécifiques"
            );
        }

        if (hasGlobalTime && hasCustomSchedules) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Vous ne pouvez pas fournir à la fois des horaires globaux ET des horaires spécifiques"
            );
        }
    }

    private void validateScheduleInPeriod(ScheduleDto scheduleDto,
                                          LocalDate eventStart, LocalDate eventEnd) {
        if (scheduleDto.getDateJour().isBefore(eventStart) ||
                scheduleDto.getDateJour().isAfter(eventEnd)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    String.format("La date %s n'est pas dans la période de l'événement (%s - %s)",
                            scheduleDto.getDateJour(), eventStart, eventEnd)
            );
        }
    }

    @Override
    public EventDto update(UUID id, EventDto eventDto) {
        return eventRepository.findById(id)
                .map(eventExisted -> {
                    validateEventDates(eventDto.getStartDate(), eventDto.getEndDate());

                    eventMapper.updateEntityFromDto(eventDto, eventExisted);
                    eventExisted.setUpdatedAt(LocalDateTime.now());

                    Event updated = eventRepository.save(eventExisted);
                    log.info("Événement mis à jour : ID = {}", id);

                    return eventMapper.toDto(updated);
                })
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Événement non trouvé avec l'ID : " + id
                ));
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventDto> allEvents() {
        List<Event> events = eventRepository.findAll();
        log.info("Récupération de {} événements", events.size());
        return eventMapper.toDtos(events);
    }

    @Override
    @Transactional(readOnly = true)
    public EventDto getEventById(UUID id) {
        return eventRepository.findById(id)
                .map(event -> {
                    log.info("Événement trouvé : ID = {}", id);
                    return eventMapper.toDto(event);
                })
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Événement non trouvé avec l'ID : " + id
                ));
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(UUID id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Événement non trouvé avec l'ID : " + id
                ));

        log.info("Suppression de l'événement : ID = {}", id);
        log.info("- {} schedules", event.getSchedules().size());
        log.info("- {} fichiers", event.getFiles().size());
        log.info("- {} participants", event.getParticipantEvents().size());

        // Supprimer les fichiers physiques AVANT la suppression en base
        if (!event.getFiles().isEmpty()) {
            // Force le chargement de la collection
            event.getFiles().forEach(file -> {
                try {
                    Path filePath = Paths.get(file.getFilePath());
                    if (Files.deleteIfExists(filePath)) {
                        log.info("✓ Fichier physique supprimé : {}", file.getFileName());
                    } else {
                        log.warn("⚠ Fichier physique introuvable : {}", file.getFilePath());
                    }
                } catch (Exception e) {
                    log.error("✗ Erreur suppression fichier {} : {}",
                            file.getFileName(), e.getMessage());
                }
            });
        }

        // Supprimer explicitement les ParticipantEvent
        if (!event.getParticipantEvents().isEmpty()) {
            log.info("Suppression de {} participantEvents", event.getParticipantEvents().size());
            participantEventRepository.deleteAll(event.getParticipantEvents());
        }

        // Supprimer explicitement les Schedule
        if (!event.getSchedules().isEmpty()) {
            log.info("Suppression de {} schedules", event.getSchedules().size());
            scheduleRepository.deleteAll(event.getSchedules());
        }

        // Supprimer explicitement les File
        if (!event.getFiles().isEmpty()) {
            log.info("Suppression de {} fichiers en base", event.getFiles().size());
            // Les fichiers seront supprimés par cascade, mais on peut le faire explicitement
            event.getFiles().clear();
        }

        // Supprimer l'événement
        eventRepository.deleteById(id);
        log.info("✓ Événement supprimé avec succès : ID = {}", id);
    }
}
