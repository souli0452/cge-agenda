package gov.bf.ascelc.cge_agenda.service.impl;

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
import gov.bf.ascelc.cge_agenda.dto.EventDto;
import gov.bf.ascelc.cge_agenda.enums.EventStatus;
import gov.bf.ascelc.cge_agenda.enums.EventType;
import gov.bf.ascelc.cge_agenda.service.EventService;
import gov.bf.ascelc.cge_agenda.service.PdfService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class PdfServiceImpl implements PdfService {

    private final EventService eventService;

    // ==========================================
    // COULEURS CHARTE GRAPHIQUE ASCELC
    // ==========================================
    private static final DeviceRgb COLOR_PRIMARY     = new DeviceRgb(34, 139, 34);   // Vert ASCELC
    private static final DeviceRgb COLOR_PRIMARY_DARK = new DeviceRgb(20, 100, 20);  // Vert foncé
    private static final DeviceRgb COLOR_PRIMARY_LIGHT= new DeviceRgb(232, 245, 232);// Vert très clair
    private static final DeviceRgb COLOR_WHITE        = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb COLOR_GRAY_LIGHT   = new DeviceRgb(248, 249, 250);
    private static final DeviceRgb COLOR_GRAY_BORDER  = new DeviceRgb(222, 226, 230);
    private static final DeviceRgb COLOR_TEXT_DARK    = new DeviceRgb(33,  37,  41);
    private static final DeviceRgb COLOR_TEXT_MUTED   = new DeviceRgb(108, 117, 125);

    // Couleurs statuts
    private static final DeviceRgb COLOR_PLANIFIE = new DeviceRgb(66,  165, 245);
    private static final DeviceRgb COLOR_EN_COURS  = new DeviceRgb(255, 167, 38);
    private static final DeviceRgb COLOR_TERMINE   = new DeviceRgb(102, 187, 106);
    private static final DeviceRgb COLOR_ANNULER   = new DeviceRgb(239, 83,  80);
    private static final DeviceRgb COLOR_REPORTER  = new DeviceRgb(171, 71,  188);

    // Formatters
    private static final DateTimeFormatter DATE_FMT  =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter MONTH_FMT =
            DateTimeFormatter.ofPattern("MMMM yyyy", Locale.FRENCH);

    // ==========================================
    // EXPORTS PAR PÉRIODE
    // ==========================================
    @Override
    public byte[] generateDailyReport(LocalDate date) {
        List<EventDto> events = eventService.getEventsByDateRange(date, date);
        return buildReport(events,
                "Événements du " + date.format(DATE_FMT),
                date.format(DATE_FMT));
    }

    @Override
    public byte[] generateWeeklyReport(LocalDate date) {
        LocalDate monday = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate sunday = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        List<EventDto> events = eventService.getEventsByDateRange(monday, sunday);
        int week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
        return buildReport(events,
                "Événements — Semaine " + week,
                monday.format(DATE_FMT) + " au " + sunday.format(DATE_FMT));
    }

    @Override
    public byte[] generateMonthlyReport(int year, int month) {
        List<EventDto> events = eventService.getEventsByMonth(year, month);
        LocalDate first = LocalDate.of(year, month, 1);
        return buildReport(events,
                "Événements de " + first.format(MONTH_FMT),
                YearMonth.of(year, month).atDay(1).format(DATE_FMT)
                        + " au " + YearMonth.of(year, month).atEndOfMonth().format(DATE_FMT));
    }

    @Override
    public byte[] generateYearlyReport(int year) {
        List<EventDto> events = eventService.getEventsByDateRange(
                LocalDate.of(year, 1, 1), LocalDate.of(year, 12, 31));
        return buildReport(events,
                "Événements de l'année " + year,
                "01/01/" + year + " au 31/12/" + year);
    }

    @Override
    public byte[] generateCustomRangeReport(LocalDate start, LocalDate end) {
        List<EventDto> events = eventService.getEventsByDateRange(start, end);
        return buildReport(events,
                "Événements de la période",
                start.format(DATE_FMT) + " au " + end.format(DATE_FMT));
    }

    @Override
    public byte[] generateFilteredReport(LocalDate start, LocalDate end,
                                         String keyword, EventType type, EventType status) {
        List<EventDto> events = eventService.getEventsByDateRange(start, end);
        if (keyword != null || type != null) {
            events = events.stream()
                    .filter(e -> (keyword == null ||
                            e.getTitle().toLowerCase().contains(keyword.toLowerCase()))
                            && (type == null || e.getType() == type))
                    .toList();
        }
        return buildReport(events, "Événements filtrés",
                start.format(DATE_FMT) + " au " + end.format(DATE_FMT));
    }

    @Override
    public byte[] generateEventsReportPDF(List<EventDto> events) {
        return buildReport(events, "Liste des événements",
                LocalDate.now().format(DATE_FMT));
    }

    @Override
    public byte[] generateCustomReport(List<EventDto> events,
                                       String title, String subtitle) {
        return buildReport(events, title, subtitle);
    }

    // ==========================================
    // MÉTHODE PRINCIPALE — CONSTRUCTION PDF
    // ==========================================
    private byte[] buildReport(List<EventDto> events, String title, String subtitle) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            PdfDocument  pdfDoc = new PdfDocument(new PdfWriter(baos));
            Document     doc    = new Document(pdfDoc, PageSize.A4.rotate());
            doc.setMargins(30, 35, 30, 35);

            PdfFont bold   = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont normal = PdfFontFactory.createFont(StandardFonts.HELVETICA);
            PdfFont italic = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);

            // En-tête institutionnel
            addInstitutionalHeader(doc, bold, normal, title, subtitle,
                    events.size());

            // Tableau des événements
            if (events.isEmpty()) {
                addEmptyMessage(doc, normal, italic);
            } else {
                addEventsTable(doc, events, bold, normal);
            }

            // Pied de page
            addFooter(doc, pdfDoc, normal, italic);

            doc.close();
            log.info("✅ PDF export généré : {} événements", events.size());
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("❌ Erreur génération PDF export : {}", e.getMessage(), e);
            throw new RuntimeException("Erreur génération PDF", e);
        }
    }

    // ==========================================
    // EN-TÊTE INSTITUTIONNEL
    // ==========================================
    private void addInstitutionalHeader(Document doc, PdfFont bold, PdfFont normal,
                                        String title, String subtitle,
                                        int count) throws Exception {
        // ── Bandeau vert plein largeur ──
        Table banner = new Table(1)
                .setWidth(UnitValue.createPercentValue(100))
                .setBorder(Border.NO_BORDER);

        Cell bannerCell = new Cell()
                .setBackgroundColor(COLOR_PRIMARY)
                .setPadding(0)
                .setBorder(Border.NO_BORDER);

        // Tableau intérieur : logo | texte central | infos droite
        Table inner = new Table(UnitValue.createPercentArray(new float[]{25, 50, 25}))
                .setWidth(UnitValue.createPercentValue(100))
                .setBorder(Border.NO_BORDER);

        // ── Colonne gauche : institution ──
        Cell left = new Cell().setBorder(Border.NO_BORDER)
                .setPadding(16).setVerticalAlignment(VerticalAlignment.MIDDLE);
        left.add(new Paragraph()
                .add(new Text("BURKINA FASO\n").setFont(bold).setFontSize(10)
                        .setFontColor(COLOR_WHITE))
                .add(new Text("─────────────────\n").setFont(normal).setFontSize(7)
                        .setFontColor(new DeviceRgb(180,220,180)))
                .add(new Text("Autorité Supérieure de\nContrôle d'État et de\nLutte contre la Corruption\n")
                        .setFont(normal).setFontSize(8).setFontColor(COLOR_WHITE))
                .add(new Text("─────────────────\n").setFont(normal).setFontSize(7)
                        .setFontColor(new DeviceRgb(180,220,180)))
                .add(new Text("ASCELC").setFont(bold).setFontSize(12)
                        .setFontColor(COLOR_WHITE))
                .setTextAlignment(TextAlignment.CENTER));
        inner.addCell(left);

        // ── Colonne centrale : titre + logo ──
        Cell center = new Cell().setBorder(Border.NO_BORDER)
                .setPadding(16).setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setTextAlignment(TextAlignment.CENTER);
        try {
            ClassPathResource logo = new ClassPathResource("static/images/logo.png");
            if (logo.exists()) {
                center.add(new Image(ImageDataFactory.create(logo.getURL()))
                        .setWidth(70).setHorizontalAlignment(HorizontalAlignment.CENTER)
                        .setMarginBottom(8));
            }
        } catch (Exception ignored) {}

        center.add(new Paragraph(title)
                .setFont(bold).setFontSize(16).setFontColor(COLOR_WHITE)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(4));
        center.add(new Paragraph(subtitle)
                .setFont(normal).setFontSize(10)
                .setFontColor(new DeviceRgb(200, 235, 200))
                .setTextAlignment(TextAlignment.CENTER));
        inner.addCell(center);

        // ── Colonne droite : résumé ──
        Cell right = new Cell().setBorder(Border.NO_BORDER)
                .setPadding(16).setVerticalAlignment(VerticalAlignment.MIDDLE);
        right.add(new Paragraph("Généré le")
                .setFont(normal).setFontSize(9)
                .setFontColor(new DeviceRgb(200,235,200))
                .setTextAlignment(TextAlignment.CENTER));
        right.add(new Paragraph(LocalDate.now().format(DATE_FMT))
                .setFont(bold).setFontSize(11).setFontColor(COLOR_WHITE)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(8));
        right.add(new Paragraph(String.valueOf(count))
                .setFont(bold).setFontSize(28).setFontColor(COLOR_WHITE)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(0));
        right.add(new Paragraph("événement" + (count > 1 ? "s" : ""))
                .setFont(normal).setFontSize(9)
                .setFontColor(new DeviceRgb(200,235,200))
                .setTextAlignment(TextAlignment.CENTER));
        inner.addCell(right);

        bannerCell.add(inner);
        banner.addCell(bannerCell);
        doc.add(banner);
        doc.add(new Paragraph("\n").setFontSize(4));
    }

    // ==========================================
    // TABLEAU DES ÉVÉNEMENTS
    // ==========================================
    private void addEventsTable(Document doc, List<EventDto> events,
                                PdfFont bold, PdfFont normal) throws Exception {
        // Colonnes : N° | Titre | Type | Dates | Horaires | Lieu | Part. | Statut
        float[] widths = {4f, 22f, 12f, 14f, 10f, 14f, 6f, 10f, 8f};
        Table table = new Table(UnitValue.createPercentArray(widths))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(4);

        // ── En-têtes ──
        String[] headers = {"N°", "Titre", "Type", "Dates", "Horaires",
                "Lieu", "Part.", "Statut", "Structures"};
        for (String h : headers) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(h).setFont(bold).setFontSize(9)
                            .setFontColor(COLOR_WHITE))
                    .setBackgroundColor(COLOR_PRIMARY)
                    .setPadding(8)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setBorder(Border.NO_BORDER)
                    .setBorderBottom(new SolidBorder(COLOR_PRIMARY_DARK, 2)));
        }

        // ── Lignes ──
        int rowNum = 0;
        for (EventDto event : events) {
            boolean isEven = rowNum++ % 2 == 0;
            DeviceRgb rowBg = isEven ? COLOR_WHITE : COLOR_GRAY_LIGHT;

            // N°
            addCell(table, String.valueOf(rowNum), normal, 9,
                    rowBg, TextAlignment.CENTER, true);

            // Titre + description
            Cell titleCell = new Cell()
                    .setBackgroundColor(rowBg).setPaddingLeft(10).setPaddingRight(6)
                    .setPaddingTop(8).setPaddingBottom(8)
                    .setBorder(Border.NO_BORDER)
                    .setBorderBottom(new SolidBorder(COLOR_GRAY_BORDER, 0.5f));
            titleCell.add(new Paragraph(event.getTitle())
                    .setFont(bold).setFontSize(9).setFontColor(COLOR_TEXT_DARK)
                    .setMarginBottom(event.getDescription() != null ? 2 : 0));
            if (event.getDescription() != null && !event.getDescription().isBlank()) {
                String desc = event.getDescription().length() > 60
                        ? event.getDescription().substring(0, 60) + "…"
                        : event.getDescription();
                titleCell.add(new Paragraph(desc)
                        .setFont(normal).setFontSize(7.5f)
                        .setFontColor(COLOR_TEXT_MUTED));
            }
            table.addCell(titleCell);

            // Type
            addCell(table, getTypeLabel(event.getType()), normal, 8,
                    rowBg, TextAlignment.CENTER, false);

            // Dates
            String dates = event.getStartDate().format(DATE_FMT);
            if (!event.getStartDate().equals(event.getEndDate())) {
                dates += "\n→ " + event.getEndDate().format(DATE_FMT);
            }
            addCell(table, dates, normal, 8, rowBg, TextAlignment.CENTER, false);

            // Horaires
            String horaires = getHorairesDisplay(event);
            addCell(table, horaires, normal, 8, rowBg, TextAlignment.CENTER, false);

            // Lieu
            String lieu = "";
            if (event.getVille() != null) lieu += event.getVille();
            if (event.getPays()  != null) {
                if (!lieu.isEmpty()) lieu += "\n";
                lieu += event.getPays();
            }
            if (event.getMeetingLink() != null) {
                if (!lieu.isEmpty()) lieu += "\n";
                lieu += "🔗 Visio";
            }
            addCell(table, lieu.isEmpty() ? "—" : lieu, normal, 8,
                    rowBg, TextAlignment.LEFT, false);

            // Participants
            int nbPart = event.getParticipants() != null
                    ? event.getParticipants().size() : 0;
            addCell(table, String.valueOf(nbPart), bold, 10,
                    rowBg, TextAlignment.CENTER, true);

            // Statut — avec couleur
            addStatusCell(table, event.getStatus(), bold, rowBg);

            // Structures
            String structures = event.getStructures() != null && !event.getStructures().isEmpty()
                    ? String.join(", ", event.getStructures()) : "—";
            if (structures.length() > 40) structures = structures.substring(0, 40) + "…";
            addCell(table, structures, normal, 7.5f, rowBg, TextAlignment.LEFT, false);
        }

        doc.add(table);
    }

    // ==========================================
    // CELLULE STANDARD
    // ==========================================
    private void addCell(Table table, String text, PdfFont font, float size,
                         DeviceRgb bg, TextAlignment align, boolean bold) {
        Paragraph p = new Paragraph(text).setFont(font).setFontSize(size)
                .setFontColor(COLOR_TEXT_DARK);
        if (bold) p.setBold();

        table.addCell(new Cell()
                .add(p)
                .setBackgroundColor(bg)
                .setPadding(7)
                .setTextAlignment(align)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(COLOR_GRAY_BORDER, 0.5f)));
    }

    // ==========================================
    // CELLULE STATUT COLORÉE
    // ==========================================
    private void addStatusCell(Table table, EventStatus status,
                               PdfFont bold, DeviceRgb rowBg) {
        DeviceRgb statusColor = getStatusColor(status);
        String    statusLabel = getStatusLabel(status);

        // Badge coloré
        Table badge = new Table(1)
                .setBorder(Border.NO_BORDER)
                .setWidth(UnitValue.createPointValue(60));
        badge.addCell(new Cell()
                .add(new Paragraph(statusLabel).setFont(bold).setFontSize(7.5f)
                        .setFontColor(COLOR_WHITE).setTextAlignment(TextAlignment.CENTER))
                .setBackgroundColor(statusColor)
                .setPaddingTop(3).setPaddingBottom(3)
                .setPaddingLeft(6).setPaddingRight(6)
                .setBorder(Border.NO_BORDER));

        Cell cell = new Cell()
                .setBackgroundColor(rowBg)
                .setPadding(5)
                .setTextAlignment(TextAlignment.CENTER)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(COLOR_GRAY_BORDER, 0.5f));
        cell.add(badge);
        table.addCell(cell);
    }

    // ==========================================
    // MESSAGE VIDE
    // ==========================================
    private void addEmptyMessage(Document doc, PdfFont normal, PdfFont italic) {
        doc.add(new Paragraph("\n\n"));
        doc.add(new Paragraph("Aucun événement pour cette période.")
                .setFont(italic).setFontSize(12)
                .setFontColor(COLOR_TEXT_MUTED)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(40));
    }

    // ==========================================
    // PIED DE PAGE
    // ==========================================
    private void addFooter(Document doc, PdfDocument pdfDoc,
                           PdfFont normal, PdfFont italic) {
        doc.add(new Paragraph("\n").setFontSize(6));

        // Ligne séparatrice
        Table line = new Table(1).setWidth(UnitValue.createPercentValue(100))
                .setBorder(Border.NO_BORDER);
        line.addCell(new Cell().setBorder(Border.NO_BORDER)
                .setBorderTop(new SolidBorder(COLOR_PRIMARY, 2))
                .setHeight(2));
        doc.add(line);

        // Texte pied de page
        Table footer = new Table(UnitValue.createPercentArray(new float[]{50, 50}))
                .setWidth(UnitValue.createPercentValue(100))
                .setBorder(Border.NO_BORDER);

        footer.addCell(new Cell().setBorder(Border.NO_BORDER).setPaddingTop(6)
                .add(new Paragraph(
                        "CGE Agenda — ASCELC | Autorité Supérieure de Contrôle d'État")
                        .setFont(normal).setFontSize(8).setFontColor(COLOR_TEXT_MUTED)));

        footer.addCell(new Cell().setBorder(Border.NO_BORDER).setPaddingTop(6)
                .add(new Paragraph(
                        "Document confidentiel — Généré le "
                                + LocalDate.now().format(DATE_FMT))
                        .setFont(italic).setFontSize(8).setFontColor(COLOR_TEXT_MUTED)
                        .setTextAlignment(TextAlignment.RIGHT)));

        doc.add(footer);
    }

    // ==========================================
    // UTILITAIRES
    // ==========================================
    private String getHorairesDisplay(EventDto event) {
        if (event.getSchedules() == null || event.getSchedules().isEmpty()) return "—";
        var first = event.getSchedules().iterator().next();
        if (first.getStartTime() == null) return "—";
        String start = first.getStartTime().toString().substring(0, 5);
        String end   = first.getEndTime()  .toString().substring(0, 5);
        return start + "\n" + end;
    }

    private DeviceRgb getStatusColor(EventStatus status) {
        if (status == null) return COLOR_TEXT_MUTED;
        return switch (status) {
            case PLANIFIE -> COLOR_PLANIFIE;
            case EN_COURS -> COLOR_EN_COURS;
            case TERMINE  -> COLOR_TERMINE;
            case ANNULER  -> COLOR_ANNULER;
            case REPORTER -> COLOR_REPORTER;
        };
    }

    private String getStatusLabel(EventStatus status) {
        if (status == null) return "—";
        return switch (status) {
            case PLANIFIE -> "Planifié";
            case EN_COURS -> "En cours";
            case TERMINE  -> "Terminé";
            case ANNULER  -> "Annulé";
            case REPORTER -> "Reporté";
        };
    }

    private String getTypeLabel(EventType type) {
        if (type == null) return "—";
        return switch (type) {
            case REUNION    -> "Réunion";
            case CONFERENCE -> "Conférence";
            case ATELIER    -> "Atelier";
            case SEMINAIRE  -> "Séminaire";
            case FORMATION  -> "Formation";
            case MISSION    -> "Mission";
            case AUDIANCE   -> "Audience";
            case AUTRE      -> "Autre";
        };
    }

    // ==========================================
    // MÉTHODES NON UTILISÉES — IMPLÉMENTÉES VIDES
    // ==========================================
    @Override public byte[] generateCalendarPDF(int year, int month)         { return new byte[0]; }
    @Override public byte[] generateMeetingReportPDF(UUID eventId)            { return new byte[0]; }
    @Override public byte[] generateAttendanceSheet(UUID eventId)             { return new byte[0]; }
    @Override public byte[] generateMonthlyStatisticsReport(int y, int m)     { return new byte[0]; }
    @Override public byte[] generateSummaryReport(LocalDate s, LocalDate e)   { return new byte[0]; }
}