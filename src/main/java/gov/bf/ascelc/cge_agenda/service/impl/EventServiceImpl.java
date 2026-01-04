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
import gov.bf.ascelc.cge_agenda.utils.ValidationUtils;

// iText PDF imports
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

// Apache POI imports (pour Excel)
import org.apache.poi.ss.usermodel.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
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
                    LocalTime globalStartTime = null;
                    LocalTime globalEndTime = null;

                    if (!event.getSchedules().isEmpty()) {
                        Schedule firstSchedule = event.getSchedules()
                                .stream()
                                .findFirst()
                                .orElse(null);

                        if (firstSchedule != null) {
                            globalStartTime = firstSchedule.getStartTime();
                            globalEndTime = firstSchedule.getEndTime();
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

        if (keyword != null && !keyword.trim().isEmpty()) {
            String lowerKeyword = keyword.toLowerCase();
            events = events.stream()
                    .filter(e -> e.getTitle().toLowerCase().contains(lowerKeyword) ||
                            (e.getDescription() != null &&
                                    e.getDescription().toLowerCase().contains(lowerKeyword)))
                    .collect(Collectors.toList());
        }

        if (type != null) {
            events = events.stream()
                    .filter(e -> e.getType() == type)
                    .collect(Collectors.toList());
        }

        if (status != null) {
            events = events.stream()
                    .filter(e -> e.getStatus() == status)
                    .collect(Collectors.toList());
        }

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

        if (participantDto.getId() != null) {
            log.info("Ajout du participant existant : ID = {}", participantDto.getId());
            participant = participantRepository.findById(participantDto.getId())
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Participant non trouvé avec l'ID : " + participantDto.getId()
                    ));
        } else {
            participant = findOrCreateParticipant(participantDto);
        }

        validateParticipantAvailability(participant, schedules);

        if (participantEventRepository.existsByParticipantIdAndEventId(
                participant.getId(), eventId)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    String.format("Le participant %s %s est déjà inscrit à cet événement",
                            participant.getFirstName(), participant.getLastName())
            );
        }

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
    // GÉNÉRER LISTE D'ÉMARGEMENT PDF
    // ==========================================
    @Override
    @Transactional(readOnly = true)
    public byte[] generateAttendanceSheet(UUID eventId) {
        log.info("📄 Génération de la liste d'émargement PDF pour l'événement : {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Événement non trouvé avec l'ID : " + eventId
                ));

        // ✅ CORRECTION : Convertir Set en List
        List<ParticipantEvent> participantEvents = new ArrayList<>(event.getParticipantEvents());

        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4);

            document.setMargins(20, 30, 30, 30);

            PdfFont fontBold = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont fontNormal = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            // EN-TÊTE
            addPdfHeader(document, fontBold, fontNormal);
            document.add(new Paragraph("\n"));

            // TITRE
            Paragraph title = new Paragraph("LISTE D'ÉMARGEMENT")
                    .setFont(fontBold)
                    .setFontSize(16)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setMarginTop(10)
                    .setMarginBottom(20)
                    .setUnderline();
            document.add(title);

            // INFORMATIONS ÉVÉNEMENT
            addEventInfoPdf(document, event, fontBold, fontNormal);
            document.add(new Paragraph("\n"));

            // TABLEAU PARTICIPANTS
            addParticipantsTablePdf(document, participantEvents, fontBold, fontNormal);

            // SIGNATURE
            addSignatureSectionPdf(document, fontNormal);

            document.close();

            log.info("✅ Liste d'émargement PDF générée avec succès");
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("❌ Erreur lors de la génération de la liste d'émargement PDF : {}", e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors de la génération de la liste d'émargement : " + e.getMessage()
            );
        }
    }

    // ==========================================
    // MÉTHODES PRIVÉES POUR PDF
    // ==========================================

    private void addPdfHeader(Document document, PdfFont fontBold, PdfFont fontNormal) throws Exception {
        // ✅ Utiliser le nom complet pour éviter conflit avec jakarta.persistence.Table
        com.itextpdf.layout.element.Table headerTable = new com.itextpdf.layout.element.Table(
                UnitValue.createPercentArray(new float[]{35, 30, 35}));
        headerTable.setWidth(UnitValue.createPercentValue(100));

        DeviceRgb borderColor = new DeviceRgb(150, 150, 150);

        // COLONNE GAUCHE
        com.itextpdf.layout.element.Cell leftCell = new com.itextpdf.layout.element.Cell()
                .setBorder(new SolidBorder(borderColor, 1))
                .setPadding(10)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);

        Paragraph leftText = new Paragraph()
                .add(new Text("AUTORITE SUPERIEURE DE\n").setFont(fontBold).setFontSize(9))
                .add(new Text("CONTROLE D'ETAT ET DE LUTTE\n").setFont(fontBold).setFontSize(9))
                .add(new Text("CONTRE LA CORRUPTION\n").setFont(fontBold).setFontSize(9))
                .add(new Text("------------\n").setFont(fontNormal).setFontSize(8))
                .add(new Text("SECRETARIAT GENERAL\n").setFont(fontBold).setFontSize(9))
                .add(new Text("------------\n").setFont(fontNormal).setFontSize(8))
                .add(new Text("DIRECTION DES SYSTEMES\n").setFont(fontBold).setFontSize(8))
                .add(new Text("D'INFORMATION, DE LA\n").setFont(fontBold).setFontSize(8))
                .add(new Text("DOCUMENTATION ET DES\n").setFont(fontBold).setFontSize(8))
                .add(new Text("ARCHIVES").setFont(fontBold).setFontSize(8))
                .setTextAlignment(TextAlignment.CENTER);

        leftCell.add(leftText);

        // COLONNE CENTRALE - LOGO
        com.itextpdf.layout.element.Cell centerCell = new com.itextpdf.layout.element.Cell()
                .setBorder(Border.NO_BORDER)
                .setPadding(5)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setHorizontalAlignment(HorizontalAlignment.CENTER);

        try {
            ClassPathResource logoResource = new ClassPathResource("static/images/logo.png");
            if (logoResource.exists()) {
                Image logo = new Image(ImageDataFactory.create(logoResource.getURL()))
                        .setWidth(80)
                        .setHorizontalAlignment(HorizontalAlignment.CENTER);
                centerCell.add(logo);
                log.info("✅ Logo chargé avec succès : logo.png");
            } else {
                log.warn("⚠️ Logo non trouvé : static/images/logo.png");
                Paragraph logoText = new Paragraph("ASCELC")
                        .setFont(fontBold)
                        .setFontSize(20)
                        .setTextAlignment(TextAlignment.CENTER)
                        .setFontColor(new DeviceRgb(0, 100, 0));
                centerCell.add(logoText);
            }
        } catch (Exception e) {
            log.warn("⚠️ Erreur chargement logo, utilisation du texte : {}", e.getMessage());
            Paragraph logoText = new Paragraph("ASCELC")
                    .setFont(fontBold)
                    .setFontSize(20)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(new DeviceRgb(0, 100, 0));
            centerCell.add(logoText);
        }

        // COLONNE DROITE
        com.itextpdf.layout.element.Cell rightCell = new com.itextpdf.layout.element.Cell()
                .setBorder(new SolidBorder(borderColor, 1))
                .setPadding(10)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);

        Paragraph rightText = new Paragraph()
                .add(new Text("BURKINA FASO\n").setFont(fontBold).setFontSize(11))
                .add(new Text("------------\n").setFont(fontNormal).setFontSize(8))
                .add(new Text("La Patrie ou la Mort,\n").setFont(fontNormal).setFontSize(9).setItalic())
                .add(new Text("nous Vaincrons").setFont(fontNormal).setFontSize(9).setItalic())
                .setTextAlignment(TextAlignment.CENTER);

        rightCell.add(rightText);

        headerTable.addCell(leftCell);
        headerTable.addCell(centerCell);
        headerTable.addCell(rightCell);

        document.add(headerTable);
    }

    private void addEventInfoPdf(Document document, Event event, PdfFont fontBold, PdfFont fontNormal) {
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        com.itextpdf.layout.element.Table infoTable = new com.itextpdf.layout.element.Table(
                UnitValue.createPercentArray(new float[]{30, 70}));
        infoTable.setWidth(UnitValue.createPercentValue(100));
        infoTable.setMarginBottom(10);

        infoTable.addCell(createInfoCell("Titre :", fontBold));
        infoTable.addCell(createInfoCell(event.getTitle(), fontNormal));

        infoTable.addCell(createInfoCell("Type :", fontBold));
        infoTable.addCell(createInfoCell(getTypeLabel(event.getType().name()), fontNormal));

        String dateInfo = event.getStartDate().format(dateFormatter);
        if (!event.getStartDate().equals(event.getEndDate())) {
            dateInfo += " au " + event.getEndDate().format(dateFormatter);
        }
        infoTable.addCell(createInfoCell("Date :", fontBold));
        infoTable.addCell(createInfoCell(dateInfo, fontNormal));

        String lieu = "";
        if (event.getVille() != null) lieu += event.getVille();
        if (event.getPays() != null) {
            if (!lieu.isEmpty()) lieu += ", ";
            lieu += event.getPays();
        }
        if (!lieu.isEmpty()) {
            infoTable.addCell(createInfoCell("Lieu :", fontBold));
            infoTable.addCell(createInfoCell(lieu, fontNormal));
        }

        document.add(infoTable);
    }

    private com.itextpdf.layout.element.Cell createInfoCell(String text, PdfFont font) {
        return new com.itextpdf.layout.element.Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(10))
                .setBorder(Border.NO_BORDER)
                .setPadding(3);
    }

    private void addParticipantsTablePdf(Document document, List<ParticipantEvent> participantEvents,
                                         PdfFont fontBold, PdfFont fontNormal) {
        if (participantEvents == null || participantEvents.isEmpty()) {
            document.add(new Paragraph("Aucun participant enregistré")
                    .setFont(fontNormal)
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.GRAY));
            return;
        }

        float[] columnWidths = {8, 30, 30, 32};
        com.itextpdf.layout.element.Table table = new com.itextpdf.layout.element.Table(
                UnitValue.createPercentArray(columnWidths));
        table.setWidth(UnitValue.createPercentValue(100));

        DeviceRgb headerColor = new DeviceRgb(220, 220, 220);

        table.addHeaderCell(createHeaderCell("N°", fontBold, headerColor));
        table.addHeaderCell(createHeaderCell("Nom et Prénoms", fontBold, headerColor));
        table.addHeaderCell(createHeaderCell("Organisation / Fonction", fontBold, headerColor));
        table.addHeaderCell(createHeaderCell("Signature", fontBold, headerColor));

        int index = 1;
        for (ParticipantEvent pe : participantEvents) {
            Participant participant = pe.getParticipant();

            table.addCell(createDataCell(String.valueOf(index++), fontNormal, TextAlignment.CENTER));

            String fullName = participant.getFirstName() + " " + participant.getLastName();
            table.addCell(createDataCell(fullName, fontNormal, TextAlignment.LEFT));

            String orgFunction = "";
            if (participant.getOrganization() != null) {
                orgFunction = participant.getOrganization();
            }
            if (participant.getJobTitle() != null) {
                if (!orgFunction.isEmpty()) orgFunction += "\n";
                orgFunction += participant.getJobTitle();
            }
            if (orgFunction.isEmpty()) orgFunction = "-";
            table.addCell(createDataCell(orgFunction, fontNormal, TextAlignment.LEFT));

            table.addCell(createDataCell("", fontNormal, TextAlignment.CENTER));
        }

        document.add(table);
    }

    private com.itextpdf.layout.element.Cell createHeaderCell(String text, PdfFont font, DeviceRgb bgColor) {
        return new com.itextpdf.layout.element.Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(9).setBold())
                .setBackgroundColor(bgColor)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(8)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 1));
    }

    private com.itextpdf.layout.element.Cell createDataCell(String text, PdfFont font, TextAlignment alignment) {
        return new com.itextpdf.layout.element.Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(9))
                .setTextAlignment(alignment)
                .setPadding(8)
                .setMinHeight(35)
                .setBorder(new SolidBorder(ColorConstants.BLACK, 0.5f));
    }

    private void addSignatureSectionPdf(Document document, PdfFont fontNormal) {
        document.add(new Paragraph("\n\n"));

        com.itextpdf.layout.element.Table signatureTable = new com.itextpdf.layout.element.Table(
                UnitValue.createPercentArray(new float[]{50, 50}));
        signatureTable.setWidth(UnitValue.createPercentValue(100));

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String today = LocalDate.now().format(dateFormatter);

        com.itextpdf.layout.element.Cell dateCell = new com.itextpdf.layout.element.Cell()
                .add(new Paragraph("Fait à Ouagadougou, le " + today)
                        .setFont(fontNormal)
                        .setFontSize(10))
                .setBorder(Border.NO_BORDER);

        com.itextpdf.layout.element.Cell signatureCell = new com.itextpdf.layout.element.Cell()
                .add(new Paragraph("Le Responsable")
                        .setFont(fontNormal)
                        .setFontSize(10)
                        .setTextAlignment(TextAlignment.CENTER))
                .add(new Paragraph("\n\n\n")
                        .setFont(fontNormal)
                        .setFontSize(10))
                .setBorder(Border.NO_BORDER);

        signatureTable.addCell(dateCell);
        signatureTable.addCell(signatureCell);

        document.add(signatureTable);
    }

    private String getTypeLabel(String type) {
        return switch (type) {
            case "REUNION" -> "Réunion";
            case "CONFERENCE" -> "Conférence";
            case "ATELIER" -> "Atelier";
            case "SEMINAIRE" -> "Séminaire";
            case "FORMATION" -> "Formation";
            case "MISSION" -> "Mission";
            case "AUTRE" -> "Autre";
            default -> type;
        };
    }

    // ==========================================
    // VÉRIFIER DISPONIBILITÉ PARTICIPANT
    // ==========================================
    @Override
    public boolean isParticipantAvailable(UUID participantId, LocalDate date,
                                          LocalTime startTime, LocalTime endTime) {
        log.info("Vérification disponibilité participant : {}", participantId);

        Participant participant = participantRepository.findById(participantId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Participant non trouvé avec l'ID : " + participantId
                ));

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

    private String getCellValue(org.apache.poi.ss.usermodel.Cell cell) {
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

    private void processParticipants(Event event, List<ParticipantDto> participantDtos,
                                     List<Schedule> newSchedules) {
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

        if (!event.getFiles().isEmpty()) {
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

        if (!event.getParticipantEvents().isEmpty()) {
            log.info("Suppression de {} participantEvents", event.getParticipantEvents().size());
            participantEventRepository.deleteAll(event.getParticipantEvents());
        }

        if (!event.getSchedules().isEmpty()) {
            log.info("Suppression de {} schedules", event.getSchedules().size());
            scheduleRepository.deleteAll(event.getSchedules());
        }

        if (!event.getFiles().isEmpty()) {
            log.info("Suppression de {} fichiers en base", event.getFiles().size());
            event.getFiles().clear();
        }

        eventRepository.deleteById(id);
        log.info("✓ Événement supprimé avec succès : ID = {}", id);
    }
}