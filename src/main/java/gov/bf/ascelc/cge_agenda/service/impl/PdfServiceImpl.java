package gov.bf.ascelc.cge_agenda.service.impl;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
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
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class PdfServiceImpl implements PdfService {

    private final EventRepository eventRepository;
    private final ParticipantEventRepository participantEventRepository;

    private static final DeviceRgb ASCELC_GREEN = new DeviceRgb(34, 139, 34);      // Vert primaire
    private static final DeviceRgb ASCELC_RED = new DeviceRgb(220, 20, 60);        // Rouge secondaire
    private static final DeviceRgb ASCELC_LIGHT_GREEN = new DeviceRgb(144, 238, 144); // Vert clair
    private static final DeviceRgb ASCELC_LIGHT_RED = new DeviceRgb(255, 182, 193);   // Rouge clair

    // Chemin du logo
    private static final String LOGO_PATH = "src/main/resources/static/images/logo.png";

    @Override
    public byte[] generateCalendarPDF(int year, int month) {
        log.info("Génération du calendrier PDF pour {}/{}", month, year);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4.rotate()); // Paysage

            // Titre
            Paragraph title = new Paragraph(
                    "Calendrier - " + getMonthName(month) + " " + year)
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(title);

            // Espacement
            document.add(new Paragraph("\n"));

            // Récupérer les événements du mois
            YearMonth yearMonth = YearMonth.of(year, month);
            LocalDate startDate = yearMonth.atDay(1);
            LocalDate endDate = yearMonth.atEndOfMonth();

            List<Event> monthEvents = eventRepository.findAll().stream()
                    .filter(e -> !e.getStartDate().isBefore(startDate) &&
                            !e.getStartDate().isAfter(endDate))
                    .sorted(Comparator.comparing(Event::getStartDate))
                    .toList();

            // Créer le calendrier
            addCalendarTable(document, year, month, monthEvents);

            // Légende
            addLegend(document);

            // Liste détaillée des événements
            addEventsList(document, monthEvents);

            document.close();
            log.info("Calendrier PDF généré avec succès");
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Erreur lors de la génération du calendrier PDF : {}", e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors de la génération du calendrier PDF"
            );
        }
    }

    private void addCalendarTable(Document document, int year, int month,
                                  List<Event> events) {
        // Créer une table 7 colonnes (jours de la semaine)
        float[] columnWidths = {1, 1, 1, 1, 1, 1, 1};
        Table table = new Table(UnitValue.createPercentArray(columnWidths));
        table.setWidth(UnitValue.createPercentValue(100));

        // En-têtes des jours
        String[] days = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
        for (String day : days) {
            Cell cell = new Cell()
                    .add(new Paragraph(day).setBold())
                    .setBackgroundColor(new DeviceRgb(200, 200, 200))
                    .setTextAlignment(TextAlignment.CENTER);
            table.addCell(cell);
        }

        // Calendrier du mois
        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDate firstDay = yearMonth.atDay(1);
        int firstDayOfWeek = firstDay.getDayOfWeek().getValue(); // 1=Lundi, 7=Dimanche

        // Cellules vides avant le premier jour
        for (int i = 1; i < firstDayOfWeek; i++) {
            table.addCell(new Cell().add(new Paragraph("")));
        }

        // Jours du mois
        for (int day = 1; day <= yearMonth.lengthOfMonth(); day++) {
            LocalDate date = yearMonth.atDay(day);
            Cell cell = createDayCell(date, events);
            table.addCell(cell);
        }

        document.add(table);
    }

    private Cell createDayCell(LocalDate date, List<Event> events) {
        Cell cell = new Cell();

        // Numéro du jour
        Paragraph dayNumber = new Paragraph(String.valueOf(date.getDayOfMonth()))
                .setBold()
                .setFontSize(12);
        cell.add(dayNumber);

        // Événements du jour
        List<Event> dayEvents = events.stream()
                .filter(e -> !e.getStartDate().isAfter(date) &&
                        !e.getEndDate().isBefore(date))
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
            case REUNION -> new DeviceRgb(173, 216, 230);      // Bleu clair
            case FORMATION -> new DeviceRgb(144, 238, 144);    // Vert clair
            case SEMINAIRE -> new DeviceRgb(255, 218, 185);    // Orange clair
            case ATELIER -> new DeviceRgb(221, 160, 221);      // Violet clair
            case CONFERENCE -> new DeviceRgb(255, 255, 153);   // Jaune clair
            case MISSION -> new DeviceRgb(255, 182, 193);      // Rose clair
            case AUTRE -> new DeviceRgb(200, 200, 200);        // Gris clair
        };
    }

    private void addLegend(Document document) {
        document.add(new Paragraph("\nLégende :").setBold());

        Table legend = new Table(2);
        legend.setWidth(UnitValue.createPercentValue(50));

        for (EventType type : EventType.values()) {
            Cell colorCell = new Cell()
                    .setBackgroundColor(getColorForType(type))
                    .setWidth(30);
            Cell typeCell = new Cell()
                    .add(new Paragraph(type.getLabel()));

            legend.addCell(colorCell);
            legend.addCell(typeCell);
        }

        document.add(legend);
    }

    private void addEventsList(Document document, List<Event> events) {
        document.add(new Paragraph("\n\nListe des événements :").setBold());

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
        return java.time.Month.of(month)
                .getDisplayName(TextStyle.FULL, Locale.FRENCH);
    }
    @Override
    public byte[] generateMeetingReportPDF(UUID eventId) {
        log.info("Génération du compte-rendu PDF pour l'événement : {}", eventId);

        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Événement non trouvé avec l'ID : " + eventId
                ));

        List<Participant> participants = participantEventRepository
                .findParticipantsByEventId(eventId);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf, PageSize.A4);

            // En-tête
            addReportHeader(document, event);

            // Informations de l'événement
            addEventInfo(document, event);

            // Liste des participants
            addParticipantsList(document, participants);

            // Section Ordre du jour
            addOrderOfDaySection(document);

            // Section Discussions
            addDiscussionsSection(document);

            // Section Décisions
            addDecisionsSection(document);

            // Section Actions
            addActionsSection(document);

            // Pied de page
            addReportFooter(document);

            document.close();
            log.info("Compte-rendu PDF généré avec succès");
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Erreur lors de la génération du compte-rendu PDF : {}", e.getMessage());
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Erreur lors de la génération du compte-rendu PDF"
            );
        }
    }

    private void addReportHeader(Document document, Event event) {
        // Titre principal
        Paragraph title = new Paragraph("COMPTE-RENDU DE RÉUNION")
                .setFontSize(18)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(title);

        // Ligne de séparation
        document.add(new Paragraph("_".repeat(80))
                .setTextAlignment(TextAlignment.CENTER));
    }

    private void addEventInfo(Document document, Event event) {
        document.add(new Paragraph("\nINFORMATIONS GÉNÉRALES").setBold().setFontSize(14));

        // Créer un tableau pour les infos
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
                .setBackgroundColor(new DeviceRgb(240, 240, 240))
                .setWidth(150);

        Cell valueCell = new Cell()
                .add(new Paragraph(value));

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private void addParticipantsList(Document document, List<Participant> participants) {
        document.add(new Paragraph("\n\nPARTICIPANTS PRÉSENTS").setBold().setFontSize(14));

        if (participants.isEmpty()) {
            document.add(new Paragraph("Aucun participant enregistré.").setItalic());
            return;
        }

        Table participantsTable = new Table(UnitValue.createPercentArray(new float[]{1, 2, 3, 2}));
        participantsTable.setWidth(UnitValue.createPercentValue(100));

        // En-têtes
        addTableHeader(participantsTable, "N°");
        addTableHeader(participantsTable, "Nom");
        addTableHeader(participantsTable, "Organisation");
        addTableHeader(participantsTable, "Fonction");

        // Données
        int index = 1;
        for (Participant p : participants) {
            participantsTable.addCell(new Cell().add(new Paragraph(String.valueOf(index++))));
            participantsTable.addCell(new Cell().add(new Paragraph(p.getFirstName() + " " + p.getLastName())));
            participantsTable.addCell(new Cell().add(new Paragraph(p.getOrganization() != null ? p.getOrganization() : "-")));
            participantsTable.addCell(new Cell().add(new Paragraph(p.getJobTitle() != null ? p.getJobTitle() : "-")));
        }

        document.add(participantsTable);
    }

    private void addTableHeader(Table table, String text) {
        Cell cell = new Cell()
                .add(new Paragraph(text).setBold())
                .setBackgroundColor(new DeviceRgb(200, 200, 200))
                .setTextAlignment(TextAlignment.CENTER);
        table.addHeaderCell(cell);
    }

    private void addOrderOfDaySection(Document document) {
        document.add(new Paragraph("\n\nORDRE DU JOUR").setBold().setFontSize(14));

        // Espace pour notes manuscrites
        addNotesBox(document, 100);
    }

    private void addDiscussionsSection(Document document) {
        document.add(new Paragraph("\n\nDISCUSSIONS / POINTS ABORDÉS").setBold().setFontSize(14));

        // Espace pour notes
        addNotesBox(document, 150);
    }

    private void addDecisionsSection(Document document) {
        document.add(new Paragraph("\n\nDÉCISIONS PRISES").setBold().setFontSize(14));

        // Tableau pour les décisions
        Table decisionsTable = new Table(UnitValue.createPercentArray(new float[]{1, 5}));
        decisionsTable.setWidth(UnitValue.createPercentValue(100));

        for (int i = 1; i <= 5; i++) {
            decisionsTable.addCell(new Cell().add(new Paragraph(String.valueOf(i))));
            decisionsTable.addCell(new Cell().setHeight(40));
        }

        document.add(decisionsTable);
    }

    private void addActionsSection(Document document) {
        document.add(new Paragraph("\n\nACTIONS À MENER").setBold().setFontSize(14));

        // Tableau pour les actions
        Table actionsTable = new Table(UnitValue.createPercentArray(new float[]{3, 2, 2}));
        actionsTable.setWidth(UnitValue.createPercentValue(100));

        addTableHeader(actionsTable, "Action");
        addTableHeader(actionsTable, "Responsable");
        addTableHeader(actionsTable, "Échéance");

        for (int i = 1; i <= 5; i++) {
            actionsTable.addCell(new Cell().setHeight(40));
            actionsTable.addCell(new Cell().setHeight(40));
            actionsTable.addCell(new Cell().setHeight(40));
        }

        document.add(actionsTable);
    }

    private void addNotesBox(Document document, float height) {
        Table notesBox = new Table(1);
        notesBox.setWidth(UnitValue.createPercentValue(100));

        Cell cell = new Cell()
                .setHeight(height)
                .setBorder(new com.itextpdf.layout.borders.SolidBorder(ColorConstants.LIGHT_GRAY, 1));

        notesBox.addCell(cell);
        document.add(notesBox);
    }

    private void addReportFooter(Document document) {
        document.add(new Paragraph("\n\n"));

        Table signatureTable = new Table(2);
        signatureTable.setWidth(UnitValue.createPercentValue(100));

        Cell cell1 = new Cell()
                .add(new Paragraph("Rédacteur :"))
                .add(new Paragraph("\n\nSignature : ___________________"))
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);

        Cell cell2 = new Cell()
                .add(new Paragraph("Date :"))
                .add(new Paragraph("\n\n___________________"))
                .setBorder(com.itextpdf.layout.borders.Border.NO_BORDER);

        signatureTable.addCell(cell1);
        signatureTable.addCell(cell2);

        document.add(signatureTable);

        // Note de bas de page
        document.add(new Paragraph("\n\nDocument généré automatiquement par CGE Agenda")
                .setFontSize(8)
                .setItalic()
                .setTextAlignment(TextAlignment.CENTER));
    }}