package gov.bf.ascelc.cge_agenda.service;

import gov.bf.ascelc.cge_agenda.dto.DashboardStatsDto;
import gov.bf.ascelc.cge_agenda.dto.MonthlyReportDto;

public interface StatsService {

    /**
     * Récupère les statistiques du dashboard
     */
    DashboardStatsDto getDashboardStats();

    /**
     * Génère un rapport mensuel
     */
    MonthlyReportDto getMonthlyReport(int year, int month);
}