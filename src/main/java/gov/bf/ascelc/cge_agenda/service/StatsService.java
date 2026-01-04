package gov.bf.ascelc.cge_agenda.service;

import gov.bf.ascelc.cge_agenda.dto.DashboardStatsDto;
import gov.bf.ascelc.cge_agenda.dto.MonthlyReportDto;

import java.util.Map;

public interface StatsService {

    /**
     * Récupère les statistiques du dashboard
     */
    DashboardStatsDto getDashboardStats();

    /**
     * Génère un rapport mensuel
     */
    MonthlyReportDto getMonthlyReport(int year, int month);

    /**
     * Récupère les événements par statut ET par mois
     * Retourne Map<Statut, Map<Mois, Count>>
     */
    Map<String, Map<String, Long>> getEventsByStatusAndMonth(int year);
}