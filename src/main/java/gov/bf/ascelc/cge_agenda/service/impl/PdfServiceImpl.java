package gov.bf.ascelc.cge_agenda.service.impl;

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
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import gov.bf.ascelc.cge_agenda.dto.EventDto;
import gov.bf.ascelc.cge_agenda.enums.EventStatus;
import gov.bf.ascelc.cge_agenda.enums.EventType;
import gov.bf.ascelc.cge_agenda.service.EventService;
import gov.bf.ascelc.cge_agenda.service.PdfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.IsoFields;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static com.itextpdf.io.font.constants.StandardFonts.HELVETICA;
import static com.itextpdf.io.font.constants.StandardFonts.HELVETICA_BOLD;

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfServiceImpl implements PdfService {

    private final EventService eventService;

    // Couleurs
    private static final DeviceRgb PRIMARY = new DeviceRgb(41, 98, 255);
    private static final DeviceRgb WHITE = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb ALT_ROW = new DeviceRgb(248, 249, 250);
    private static final DeviceRgb BORDER = new DeviceRgb(222, 226, 230);
    private static final DeviceRgb TEXT_DARK = new DeviceRgb(33, 37, 41);
    private static final DeviceRgb TEXT_LIGHT = new DeviceRgb(108, 117, 125);

    // Formatters
    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private final DateTimeFormatter monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);

    // ==========================================
    // EXPORTS PAR PÉRIODE FIXE
    // ==========================================

    @Override
    public byte[] generateDailyReport(LocalDate date) {
        log.info("📅 Génération rapport journalier: {}", date);

        List<EventDto> events = eventService.getEventsByDateRange(date, date);
        String title = String.format("Événements du %s", date.format(dateFormatter));
        String subtitle = String.format("%d événement(s)", events.size());

        return buildPdfReport(events, title, subtitle);
    }

    @Override
    public byte[] generateWeeklyReport(LocalDate date) {
        log.info("📅 Génération rapport hebdomadaire pour la semaine contenant: {}", date);

        // Calculer le lundi et le dimanche
        LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));

        List<EventDto> events = eventService.getEventsByDateRange(monday, sunday);

        int weekNumber = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        String title = String.format("Événements de la semaine %d", weekNumber);
        String subtitle = String.format("%s au %s • %d événement(s)",
                monday.format(dateFormatter),
                sunday.format(dateFormatter),
                events.size());

        return buildPdfReport(events, title, subtitle);
    }

    @Override
    public byte[] generateMonthlyReport(int year, int month) {
        log.info("📅 Génération rapport mensuel: {}/{}", month, year);

        List<EventDto> events = eventService.getEventsByMonth(year, month);

        LocalDate firstDay = LocalDate.of(year, month, 1);
        String title = String.format("Événements de %s", firstDay.format(monthFormatter));
        String subtitle = String.format("%d événement(s)", events.size());

        return buildPdfReport(events, title, subtitle);
    }

    @Override
    public byte[] generateYearlyReport(int year) {
        log.info("📅 Génération rapport annuel: {}", year);

        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end = LocalDate.of(year, 12, 31);

        List<EventDto> events = eventService.getEventsByDateRange(start, end);

        String title = String.format("Événements de l'année %d", year);
        String subtitle = String.format("%d événement(s)", events.size());

        return buildPdfReport(events, title, subtitle);
    }

    // ==========================================
    // EXPORT PAR PLAGE PERSONNALISÉE
    // ==========================================

    @Override
    public byte[] generateCustomRangeReport(LocalDate startDate, LocalDate endDate) {
        log.info("📅 Génération rapport personnalisé: {} à {}", startDate, endDate);

        List<EventDto> events = eventService.getEventsByDateRange(startDate, endDate);

        String title = "Événements de la période";
        String subtitle = String.format("%s au %s • %d événement(s)",
                startDate.format(dateFormatter),
                endDate.format(dateFormatter),
                events.size());

        return buildPdfReport(events, title, subtitle);
    }

    @Override
    public byte[] generateFilteredReport(LocalDate startDate, LocalDate endDate,
                                         String keyword, EventType type, EventType status) {
        log.info("📅 Génération rapport filtré: {} à {}", startDate, endDate);

        List<EventDto> events = eventService.getEventsByDateRange(startDate, endDate);

        // Application des filtres (vous pouvez utiliser searchEvents au lieu)
        if (keyword != null || type != null) {
            events = events.stream()
                    .filter(e -> {
                        boolean matchKeyword = keyword == null ||
                                e.getTitle().toLowerCase().contains(keyword.toLowerCase());
                        boolean matchType = type == null || e.getType() == type;
                        return matchKeyword && matchType;
                    })
                    .toList();
        }

        String title = "Événements filtrés";
        String subtitle = String.format("%s au %s • %d événement(s)",
                startDate.format(dateFormatter),
                endDate.format(dateFormatter),
                events.size());

        return buildPdfReport(events, title, subtitle);
    }

    // ==========================================
    // EXPORT AVEC LISTE D'ÉVÉNEMENTS
    // ==========================================

    @Override
    public byte[] generateEventsReportPDF(List<EventDto> events) {
        log.info("📅 Génération rapport avec {} événements", events.size());

        String title = "Liste des événements";
        String subtitle = String.format("%d événement(s)", events.size());

        return buildPdfReport(events, title, subtitle);
    }

    @Override
    public byte[] generateCustomReport(List<EventDto> events, String title, String subtitle) {
        log.info("📅 Génération rapport personnalisé: {}", title);
        return buildPdfReport(events, title, subtitle);
    }

    // ==========================================
    // MÉTHODE PRINCIPALE DE CONSTRUCTION PDF
    // ==========================================

    private byte[] buildPdfReport(List<EventDto> events, String title, String subtitle) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4.rotate());

            document.setMargins(40, 40, 40, 40);

            PdfFont boldFont = PdfFontFactory.createFont(HELVETICA_BOLD);
            PdfFont regularFont = PdfFontFactory.createFont(HELVETICA);

            // EN-TÊTE
            addHeader(document, boldFont, regularFont, title, subtitle);

            // TABLEAU
            if (events.isEmpty()) {
                addEmptyMessage(document, regularFont);
            } else {
                addEventsTable(document, events, boldFont, regularFont);
            }

            // PIED DE PAGE
            addFooter(document, regularFont);

            document.close();

            log.info("✅ PDF généré avec succès");
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("❌ Erreur génération PDF: {}", e.getMessage(), e);
            throw new RuntimeException("Erreur lors de la génération du PDF", e);
        }
    }

    private void addHeader(Document document, PdfFont boldFont, PdfFont regularFont,
                           String title, String subtitle) throws Exception {
        // Titre
        Paragraph titlePara = new Paragraph(title.toUpperCase())
                .setFont(boldFont)
                .setFontSize(22)
                .setFontColor(TEXT_DARK)
                .setTextAlignment(TextAlignment.CENTER)
                .setBold()
                .setMarginBottom(5);
        document.add(titlePara);

        // Sous-titre
        Paragraph subPara = new Paragraph(subtitle)
                .setFont(regularFont)
                .setFontSize(11)
                .setFontColor(TEXT_LIGHT)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20);
        document.add(subPara);

        // Ligne de séparation
        Table line = new Table(1)
                .setWidth(UnitValue.createPercentValue(100))
                .setBorder(Border.NO_BORDER)
                .setMarginBottom(15);
        line.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(PRIMARY, 3)));
        document.add(line);
    }

    private void addEventsTable(Document document, List<EventDto> events,
                                PdfFont boldFont, PdfFont regularFont) throws Exception {
        float[] columnWidths = {12f, 18f, 23f, 12f, 8f, 27f};
        Table table = new Table(UnitValue.createPercentArray(columnWidths))
                .setWidth(UnitValue.createPercentValue(100));

        // EN-TÊTE
        String[] headers = {"Date", "Titre", "Lieu", "Statut", "Part.", "Structures"};
        for (String header : headers) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(header).setFont(boldFont).setFontSize(11))
                    .setBackgroundColor(PRIMARY)
                    .setFontColor(WHITE)
                    .setPadding(10)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBorder(Border.NO_BORDER));
        }

        // DONNÉES
        int row = 0;
        for (EventDto event : events) {
            DeviceRgb bg = row++ % 2 == 0 ? WHITE : ALT_ROW;

            addCell(table, formatDateRange(event), regularFont, 10, bg, TextAlignment.CENTER);
            addCell(table, event.getTitle(), boldFont, 10, bg, TextAlignment.LEFT);
            addCell(table, formatLocation(event), regularFont, 9, bg, TextAlignment.LEFT);
            addCell(table, formatStatus(event.getStatus()), regularFont, 9, bg, TextAlignment.CENTER);
            addCell(table, String.valueOf(event.getParticipants() != null ? event.getParticipants().size() : 0),
                    boldFont, 10, bg, TextAlignment.CENTER);
            addCell(table, formatStructures(event), regularFont, 9, bg, TextAlignment.LEFT);
        }

        document.add(table);
    }

    private void addCell(Table table, String text, PdfFont font, float size,
                         DeviceRgb bg, TextAlignment align) {
        table.addCell(new Cell()
                .add(new Paragraph(text).setFont(font).setFontSize(size))
                .setBackgroundColor(bg)
                .setPadding(8)
                .setTextAlignment(align)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(BORDER, 1)));
    }

    private void addEmptyMessage(Document document, PdfFont font) {
        document.add(new Paragraph("Aucun événement pour cette période")
                .setFont(font)
                .setFontSize(12)
                .setTextAlignment(TextAlignment.CENTER)
                .setFontColor(TEXT_LIGHT)
                .setMarginTop(40));
    }

    private void addFooter(Document document, PdfFont font) {
        document.add(new Paragraph("Document généré automatiquement - Confidentiel")
                .setFont(font)
                .setFontSize(9)
                .setFontColor(TEXT_LIGHT)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(20)
                .setItalic());
    }

    // ==========================================
    // UTILITAIRES DE FORMATAGE
    // ==========================================

    private String formatDateRange(EventDto event) {
        LocalDate start = event.getStartDate();
        LocalDate end = event.getEndDate();
        return start.equals(end) ? start.format(dateFormatter) :
                start.format(dateFormatter) + " -\n" + end.format(dateFormatter);
    }

    private String formatLocation(EventDto event) {
        StringBuilder loc = new StringBuilder();
        if (event.getVille() != null) loc.append(event.getVille());
        if (event.getPays() != null) {
            if (loc.length() > 0) loc.append(", ");
            loc.append(event.getPays());
        }
        if (event.getMeetingLink() != null) {
            if (loc.length() > 0) loc.append("\n");
            loc.append("🔗 Visio");
        }
        return loc.length() > 0 ? loc.toString() : "-";
    }
    private String formatStatus(EventStatus status) {
        return switch (status.name()) {
            case "PLANIFIE" -> "Planifié";
            case "EN_COURS" -> "En cours";
            case "TERMINE" -> "Terminé";
            case "ANNULER" -> "Annulé";
            case "REPORTER" -> "Reporté";
            default -> status.name();
        };
    }

    private String formatStructures(EventDto event) {
        return event.getStructures() != null && !event.getStructures().isEmpty() ?
                String.join(", ", event.getStructures()) : "-";
    }

    // ==========================================
    // DOCUMENTS SPÉCIAUX (À IMPLÉMENTER)
    // ==========================================

    @Override
    public byte[] generateCalendarPDF(int year, int month) {
        log.info("📅 Calendrier visuel: {}/{} (à implémenter)", month, year);
        return new byte[0];
    }

    @Override
    public byte[] generateMeetingReportPDF(UUID eventId) {
        log.info("📝 Compte-rendu: {} (à implémenter)", eventId);
        return new byte[0];
    }

    @Override
    public byte[] generateAttendanceSheet(UUID eventId) {
        log.info("✍️ Liste émargement: {} (à implémenter)", eventId);
        return new byte[0];
    }

    @Override
    public byte[] generateMonthlyStatisticsReport(int year, int month) {
        log.info("📊 Statistiques: {}/{} (à implémenter)", month, year);
        return new byte[0];
    }

    @Override
    public byte[] generateSummaryReport(LocalDate startDate, LocalDate endDate) {
        log.info("📋 Synthèse: {} à {} (à implémenter)", startDate, endDate);
        return new byte[0];
    }
}