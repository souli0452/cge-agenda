package gov.bf.ascelc.cge_agenda.scheduler;

import gov.bf.ascelc.cge_agenda.entities.BackupConfig;
import gov.bf.ascelc.cge_agenda.repository.BackupConfigRepository;
import gov.bf.ascelc.cge_agenda.service.BackupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class BackupScheduler {

    private final BackupConfigRepository backupConfigRepository;
    private final BackupService backupService;

    /**
     * Injectable pour les tests (ReflectionTestUtils). En production, l'horloge
     * système par défaut.
     */
    private final Clock clock = Clock.systemDefaultZone();

    /**
     * Vérifie chaque minute si l'heure planifiée pour la sauvegarde automatique
     * est atteinte ou dépassée pour aujourd'hui, et si aucune sauvegarde n'a
     * encore eu lieu depuis cette heure. Ce rattrapage (plutôt qu'une simple
     * égalité d'heure/minute) évite qu'un redémarrage de l'application pile au
     * moment planifié ne fasse silencieusement sauter la sauvegarde du jour.
     */
    @Scheduled(cron = "0 * * * * *")
    public void checkAndRunScheduledBackup() {
        BackupConfig config = backupConfigRepository.findFirstByOrderByCreatedAtAsc().orElse(null);
        if (config == null || !config.isAutoEnabled()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime scheduledToday = now.toLocalDate().atTime(config.getBackupHour(), config.getBackupMinute());

        if (now.isBefore(scheduledToday)) {
            return;
        }

        LocalDateTime lastBackup = backupService.lastBackupAt().orElse(null);
        if (lastBackup != null && !lastBackup.isBefore(scheduledToday)) {
            return;
        }

        try {
            backupService.runScheduledBackup();
        } catch (Exception e) {
            log.error("❌ Échec de la sauvegarde automatique planifiée : {}", e.getMessage(), e);
        }
    }
}
