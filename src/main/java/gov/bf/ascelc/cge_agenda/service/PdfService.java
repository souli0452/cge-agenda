package gov.bf.ascelc.cge_agenda.service;

public interface PdfService {

    /**
     * Génère un calendrier PDF pour un mois donné
     */
    byte[] generateCalendarPDF(int year, int month);

    /**
     * Génère un compte-rendu de réunion PDF
     */
    byte[] generateMeetingReportPDF(java.util.UUID eventId);
}