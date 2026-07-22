package gov.bf.ascelc.cge_agenda.scheduler;

import gov.bf.ascelc.cge_agenda.entities.BackupConfig;
import gov.bf.ascelc.cge_agenda.repository.BackupConfigRepository;
import gov.bf.ascelc.cge_agenda.service.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class BackupScheduler {

    private final BackupConfigRepository backupConfigRepository;
    private final BackupService backupService;

    private int lastRunKey = -1;

    /**
     * Vérifie chaque minute si c'est l'heure configurée pour la sauvegarde automatique.
     */
    @Scheduled(cron = "0 * * * * *")
    public void checkAndRunScheduledBackup() {
        BackupConfig config = backupConfigRepository.findFirstByOrderByCreatedAtAsc().orElse(null);
        if (config == null || !config.isAutoEnabled()) {
            return;
        }

        LocalTime now = LocalTime.now();
        if (now.getHour() != config.getBackupHour() || now.getMinute() != config.getBackupMinute()) {
            return;
        }

        int todayKey = java.time.LocalDate.now().getDayOfYear() * 24 * 60 + now.getHour() * 60 + now.getMinute();
        if (todayKey == lastRunKey) {
            return;
        }
        lastRunKey = todayKey;

        try {
            backupService.runScheduledBackup();
        } catch (Exception e) {
            log.error("❌ Échec de la sauvegarde automatique planifiée : {}", e.getMessage(), e);
        }
    }
}
