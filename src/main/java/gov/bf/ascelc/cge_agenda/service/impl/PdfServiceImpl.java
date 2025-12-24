package gov.bf.ascelc.cge_agenda.service.impl;

import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.LineSeparator;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import gov.bf.ascelc.cge_agenda.entities.Event;
import gov.bf.ascelc.cge_agenda.entities.Participant;
import gov.bf.ascelc.cge_agenda.enums.EventType;
import gov.bf.ascelc.cge_agenda.repository.EventRepository;
import gov.bf.ascelc.cge_agenda.repository.ParticipantEventRepository;
import gov.bf.ascelc.cge_agenda.service.PdfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PdfServiceImpl implements PdfService {

    private final EventRepository eventRepository;
    private final ParticipantEventRepository participantEventRepository;

    private static final DeviceRgb ASCELC_GREEN = new DeviceRgb(34, 139, 34);
    private static final DeviceRgb ASCELC_RED = new DeviceRgb(220, 20, 60);
    private static final DeviceRgb ASCELC_LIGHT_GREEN = new DeviceRgb(144, 238, 144);
    private static final DeviceRgb ASCELC_LIGHT_RED = new DeviceRgb(255, 182, 193);
    private static final String LOGO_PATH = "src/main/resources/static/images/logo.png";

    @Override
    public byte[] generateCalendarPDF(int year, int month) {
        log.info("Génération du calendrier PDF pour {}/{}", month, year);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4.rotate());

            // Titre en vert ASCELC
            Paragraph title = new Paragraph("Calendrier - " + getMonthName(month) + " " + year)
                    .setFontSize(20)
                    .setBold()
                    .setFontColor(ASCELC_GREEN)
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(title);
            document.add(new Paragraph("\n"));

            YearMonth yearMonth = YearMonth.of(year, month);
            LocalDate startDate = yearMonth.atDay(1);
            LocalDate endDate = yearMonth.atEndOfMonth();

            List<Event> monthEvents = eventRepository.findAll().stream()
                    .filter(e -> !e.getStartDate().isBefore(startDate) &&
                            !e.getStartDate().isAfter(endDate))
                    .sorted(Comparator.comparing(Event::getStartDate))
                    .toList();

            addCalendarTable(document, year, month, monthEvents);
            addLegend(document);
            addEventsList(document, monthEvents);

            document.close();
            log.info("Calendrier PDF généré avec succès");
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Erreur lors de la génération du calendrier PDF : {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors de la génération du calendrier PDF");
        }
    }

    private void addCalendarTable(Document document, int year, int month, List<Event> events) {
        float[] columnWidths = {1, 1, 1, 1, 1, 1, 1};
        Table table = new Table(UnitValue.createPercentArray(columnWidths));
        table.setWidth(UnitValue.createPercentValue(100));

        String[] days = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
        for (String day : days) {
            Cell cell = new Cell()
                    .add(new Paragraph(day).setBold().setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(ASCELC_GREEN)
                    .setTextAlignment(TextAlignment.CENTER);
            table.addCell(cell);
        }

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate firstDay = yearMonth.atDay(1);
        int firstDayOfWeek = firstDay.getDayOfWeek().getValue();

        for (int i = 1; i < firstDayOfWeek; i++) {
            table.addCell(new Cell().add(new Paragraph("")));
        }

        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate date = yearMonth.atDay(day);
            Cell cell = createDayCell(date, events);
            table.addCell(cell);
        }

        document.add(table);
    }

    private Cell createDayCell(LocalDate date, List<Event> events) {
        Cell cell = new Cell();
        Paragraph dayNumber = new Paragraph(String.valueOf(date.getDayOfMonth()))
                .setBold()
                .setFontSize(12);
        cell.add(dayNumber);

        List<Event> dayEvents = events.stream()
                .filter(e -> !e.getStartDate().isAfter(date) && !e.getEndDate().isBefore(date))
                .toList();

        for (Event event : dayEvents) {
            Paragraph eventPara = new Paragraph(event.getTitle())
                    .setFontSize(8)
                    .setBackgroundColor(getColorForType(event.getType()))
                    .setMarginTop(2);
            cell.add(eventPara);
        }

        return cell;
    }

    private DeviceRgb getColorForType(EventType type) {
        return switch (type) {
            case REUNION -> new DeviceRgb(173, 216, 230);
            case FORMATION -> new DeviceRgb(144, 238, 144);
            case SEMINAIRE -> new DeviceRgb(255, 218, 185);
            case ATELIER -> new DeviceRgb(221, 160, 221);
            case CONFERENCE -> new DeviceRgb(255, 255, 153);
            case MISSION -> new DeviceRgb(255, 182, 193);
            case AUTRE -> new DeviceRgb(200, 200, 200);
        };
    }

    private void addLegend(Document document) {
        document.add(new Paragraph("\nLégende :").setBold().setFontColor(ASCELC_GREEN));

        Table legend = new Table(2);
        legend.setWidth(UnitValue.createPercentValue(50));

        for (EventType type : EventType.values()) {
            Cell colorCell = new Cell()
                    .setBackgroundColor(getColorForType(type))
                    .setWidth(30);
            Cell typeCell = new Cell().add(new Paragraph(type.getLabel()));
            legend.addCell(colorCell);
            legend.addCell(typeCell);
        }

        document.add(legend);
    }

    private void addEventsList(Document document, List<Event> events) {
        document.add(new Paragraph("\n\nListe des événements :").setBold().setFontColor(ASCELC_GREEN));

        for (Event event : events) {
            Paragraph eventDetails = new Paragraph()
                    .add(event.getStartDate().toString() + " - ")
                    .add(new com.itextpdf.layout.element.Text(event.getTitle()).setBold())
                    .add(" (" + event.getType().getLabel() + ")");

            if (event.getDescription() != null) {
                eventDetails.add("\n  " + event.getDescription());
            }
            document.add(eventDetails);
        }
    }

    private String getMonthName(int month) {
        return java.time.Month.of(month).getDisplayName(TextStyle.FULL, Locale.FRENCH);
    }

    @Override
    public byte[] generateMeetingReportPDF(UUID eventId) {
        log.info("Génération du compte-rendu PDF pour l'événement : {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "Événement non trouvé avec l'ID : " + eventId));

        List<Participant> participants = participantEventRepository.findParticipantsByEventId(eventId);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);

            addReportHeader(document, event);
            addEventInfo(document, event);
            addParticipantsList(document, participants);
            addOrderOfDaySection(document);
            addDiscussionsSection(document);
            addDecisionsSection(document);
            addActionsSection(document);
            addReportFooter(document);

            document.close();
            log.info("Compte-rendu PDF généré avec succès");
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Erreur lors de la génération du compte-rendu PDF : {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors de la génération du compte-rendu PDF");
        }
    }

    private void addReportHeader(Document document, Event event) {
        try {
            Path logoPath = Paths.get(LOGO_PATH);
            if (Files.exists(logoPath)) {
                Image logo = new Image(ImageDataFactory.create(logoPath.toString()));
                logo.setWidth(80);
                logo.setHorizontalAlignment(HorizontalAlignment.LEFT);
                document.add(logo);
            }
        } catch (Exception e) {
            log.warn("Impossible de charger le logo : {}", e.getMessage());
        }

        Paragraph title = new Paragraph("COMPTE-RENDU DE RÉUNION")
                .setFontSize(20)
                .setBold()
                .setFontColor(ASCELC_GREEN)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10)
                .setMarginBottom(10);
        document.add(title);

        Paragraph subtitle = new Paragraph("Agence de Services aux Collectivités Locales")
                .setFontSize(10)
                .setItalic()
                .setFontColor(ASCELC_GREEN)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(15);
        document.add(subtitle);

        SolidLine line = new SolidLine();
        line.setColor(ASCELC_GREEN);
        line.setLineWidth(2);
        LineSeparator separator = new LineSeparator(line);
        document.add(separator);
        document.add(new Paragraph("\n"));
    }

    private void addEventInfo(Document document, Event event) {
        Paragraph sectionTitle = new Paragraph("INFORMATIONS GÉNÉRALES")
                .setBold()
                .setFontSize(14)
                .setFontColor(ASCELC_GREEN)
                .setMarginTop(10);
        document.add(sectionTitle);

        Table infoTable = new Table(2);
        infoTable.setWidth(UnitValue.createPercentValue(100));

        addInfoRow(infoTable, "Titre", event.getTitle());
        addInfoRow(infoTable, "Type", event.getType().getLabel());
        addInfoRow(infoTable, "Date", event.getStartDate().toString());
        addInfoRow(infoTable, "Horaire",
                event.getSchedules().isEmpty() ? "Non défini" :
                        event.getSchedules().stream().findFirst().get().getStartTime() +
                                " - " +
                                event.getSchedules().stream().findFirst().get().getEndTime());

        if (event.getMeetingLink() != null) {
            addInfoRow(infoTable, "Lien", event.getMeetingLink());
        }

        document.add(infoTable);
    }

    private void addInfoRow(Table table, String label, String value) {
        Cell labelCell = new Cell()
                .add(new Paragraph(label + " :").setBold())
                .setBackgroundColor(ASCELC_LIGHT_GREEN)
                .setFontColor(ColorConstants.BLACK)
                .setWidth(150);

        Cell valueCell = new Cell()
                .add(new Paragraph(value))
                .setBorder(new SolidBorder(ASCELC_GREEN, 0.5f));

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addParticipantsList(Document document, List<Participant> participants) {
        Paragraph sectionTitle = new Paragraph("PARTICIPANTS PRÉSENTS")
                .setBold()
                .setFontSize(14)
                .setFontColor(ASCELC_GREEN)
                .setMarginTop(15);
        document.add(sectionTitle);

        if (participants.isEmpty()) {
            document.add(new Paragraph("Aucun participant enregistré.").setItalic());
            return;
        }

        Table participantsTable = new Table(UnitValue.createPercentArray(new float[]{1, 2, 3, 2}));
        participantsTable.setWidth(UnitValue.createPercentValue(100));

        addTableHeader(participantsTable, "N°");
        addTableHeader(participantsTable, "Nom");
        addTableHeader(participantsTable, "Organisation");
        addTableHeader(participantsTable, "Fonction");

        int index = 1;
        for (Participant p : participants) {
            participantsTable.addCell(createStyledCell(String.valueOf(index++)));
            participantsTable.addCell(createStyledCell(p.getFirstName() + " " + p.getLastName()));
            participantsTable.addCell(createStyledCell(p.getOrganization() != null ? p.getOrganization() : "-"));
            participantsTable.addCell(createStyledCell(p.getJobTitle() != null ? p.getJobTitle() : "-"));
        }

        document.add(participantsTable);
    }

    private Cell createStyledCell(String text) {
        return new Cell()
                .add(new Paragraph(text))
                .setBorder(new SolidBorder(ASCELC_LIGHT_GREEN, 0.5f));
    }

    private void addTableHeader(Table table, String text) {
        Cell cell = new Cell()
                .add(new Paragraph(text).setBold().setFontColor(ColorConstants.WHITE))
                .setBackgroundColor(ASCELC_GREEN)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(8);
        table.addHeaderCell(cell);
    }

    private void addOrderOfDaySection(Document document) {
        Paragraph sectionTitle = new Paragraph("ORDRE DU JOUR")
                .setBold()
                .setFontSize(14)
                .setFontColor(ASCELC_GREEN)
                .setMarginTop(15);
        document.add(sectionTitle);
        addNotesBox(document, 100);
    }

    private void addDiscussionsSection(Document document) {
        Paragraph sectionTitle = new Paragraph("DISCUSSIONS / POINTS ABORDÉS")
                .setBold()
                .setFontSize(14)
                .setFontColor(ASCELC_GREEN)
                .setMarginTop(15);
        document.add(sectionTitle);
        addNotesBox(document, 150);
    }

    private void addDecisionsSection(Document document) {
        Paragraph sectionTitle = new Paragraph("DÉCISIONS PRISES")
                .setBold()
                .setFontSize(14)
                .setFontColor(ASCELC_GREEN)
                .setMarginTop(15);
        document.add(sectionTitle);

        Table decisionsTable = new Table(UnitValue.createPercentArray(new float[]{1, 5}));
        decisionsTable.setWidth(UnitValue.createPercentValue(100));

        for (int i = 1; i <= 5; i++) {
            Cell numberCell = new Cell()
                    .add(new Paragraph(String.valueOf(i)).setBold())
                    .setBackgroundColor(ASCELC_LIGHT_GREEN)
                    .setTextAlignment(TextAlignment.CENTER);

            Cell contentCell = new Cell()
                    .setHeight(40)
                    .setBorder(new SolidBorder(ASCELC_GREEN, 0.5f));

            decisionsTable.addCell(numberCell);
            decisionsTable.addCell(contentCell);
        }

        document.add(decisionsTable);
    }

    private void addActionsSection(Document document) {
        Paragraph sectionTitle = new Paragraph("ACTIONS À MENER")
                .setBold()
                .setFontSize(14)
                .setFontColor(ASCELC_GREEN)
                .setMarginTop(15);
        document.add(sectionTitle);

        Table actionsTable = new Table(UnitValue.createPercentArray(new float[]{3, 2, 2}));
        actionsTable.setWidth(UnitValue.createPercentValue(100));

        addTableHeader(actionsTable, "Action");
        addTableHeader(actionsTable, "Responsable");
        addTableHeader(actionsTable, "Échéance");

        for (int i = 1; i <= 5; i++) {
            actionsTable.addCell(new Cell().setHeight(40).setBorder(new SolidBorder(ASCELC_GREEN, 0.5f)));
            actionsTable.addCell(new Cell().setHeight(40).setBorder(new SolidBorder(ASCELC_GREEN, 0.5f)));
            actionsTable.addCell(new Cell().setHeight(40).setBorder(new SolidBorder(ASCELC_GREEN, 0.5f)));
        }

        document.add(actionsTable);
    }

    private void addNotesBox(Document document, float height) {
        Table notesBox = new Table(1);
        notesBox.setWidth(UnitValue.createPercentValue(100));

        Cell cell = new Cell()
                .setHeight(height)
                .setBorder(new SolidBorder(ASCELC_GREEN, 1));

        notesBox.addCell(cell);
        document.add(notesBox);
    }

    private void addReportFooter(Document document) {
        document.add(new Paragraph("\n\n"));

        Table signatureTable = new Table(2);
        signatureTable.setWidth(UnitValue.createPercentValue(100));

        Cell cell1 = new Cell()
                .add(new Paragraph("Rédacteur :").setBold())
                .add(new Paragraph("\n\nSignature : ___________________"))
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);

        Cell cell2 = new Cell()
                .add(new Paragraph("Date :").setBold())
                .add(new Paragraph("\n\n___________________"))
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);

        signatureTable.addCell(cell1);
        signatureTable.addCell(cell2);

        document.add(signatureTable);
        SolidLine line = new SolidLine();
        line.setColor(ASCELC_GREEN);
        line.setLineWidth(1);
        LineSeparator separator = new LineSeparator(line);
        document.add(separator);

        document.add(new Paragraph("\nDocument généré automatiquement par CGE Agenda - ASCELC")
                .setFontSize(8)
                .setItalic()
                .setFontColor(ASCELC_GREEN)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10));
    }
}