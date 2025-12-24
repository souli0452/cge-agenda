package gov.bf.ascelc.cge_agenda.controller;

import gov.bf.ascelc.cge_agenda.dto.DashboardStatsDto;
import gov.bf.ascelc.cge_agenda.dto.MonthlyReportDto;
import gov.bf.ascelc.cge_agenda.service.StatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static gov.bf.ascelc.cge_agenda.utils.ApiUrls.*;

@RestController
@RequestMapping(STATS_ROOT_URL)
@RequiredArgsConstructor
@Slf4j
public class StatsController {

    private final StatsService statsService;

    /**
     * Récupère les statistiques du dashboard
     * GET /api/v1/cge-agenda/stats/dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<DashboardStatsDto> getDashboardStats() {
        log.info("Récupération des statistiques du dashboard");
        DashboardStatsDto stats = statsService.getDashboardStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Génère un rapport mensuel
     * GET /api/v1/cge-agenda/stats/monthly/{year}/{month}
     */
    @GetMapping("/monthly/{year}/{month}")
    public ResponseEntity<MonthlyReportDto> getMonthlyReport(
            @PathVariable int year,
            @PathVariable int month
    ) {
        log.info("Génération du rapport mensuel : {}/{}", month, year);
        MonthlyReportDto report = statsService.getMonthlyReport(year, month);
        return ResponseEntity.ok(report);
    }
}